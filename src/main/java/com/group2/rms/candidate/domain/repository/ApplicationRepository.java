package com.group2.rms.candidate.domain.repository;

import com.group2.rms.candidate.pipeline.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.pipeline.dto.JobPostingOption;
import com.group2.rms.candidate.domain.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {
        // check hr recommend
        String HR_RECOMMENDATION = """
                exists (select l.auditLogId from AuditLog l where l.entityName = 'Application'
                    and l.entityId = cast(a.applicationId as string) and l.newValue = 'TalentPoolHrRecommended')
                """;
        //check if review from HR with hold and HM not review before
        String PENDING_HR_RECOMMENDATION = HR_RECOMMENDATION + """
                and exists (select rv.reviewId from ApplicationReview rv where rv.application = a
                    and rv.reviewerRole = 'HR' and rv.decision = 'Hold')
                and not exists (select rv.reviewId from ApplicationReview rv where rv.application = a
                    and rv.reviewerRole = 'HiringManager' and rv.decision in ('Pass','Fail'))
                """;
        //check if not HM_suggest and not talent pool cancelled
        String FORMAL_FILTER = """
                        a.applicationStatus <> 'HM_Suggested'
                        and not exists (select l.auditLogId from AuditLog l
                            where l.entityName = 'Application' and l.entityId = cast(a.applicationId as string)
                              and l.newValue = 'TalentPoolCancelled')
                        """;

        /**
         * Đơn → ứng viên → tin → requisition → phòng ban (+ trưởng phòng), dùng chung
         * cho các query danh sách.
         */
        String PIPELINE_JOINS = """
                        from Application a
                            join a.candidate c
                            join c.account u
                            join a.jobPosting jp
                            join jp.requisition r
                            left join r.department d
                            left join d.manager m
                        """;

        /**
         * Phạm vi người xem ({@code ApplicationScope}): HM thấy mọi đơn của phòng ban
         * mình quản lý hoặc trực thuộc, kể cả đơn HR chưa chuyển.
         */
        String SCOPE_FILTER = """
                        where (:allDepartments = true or m.userId = :managerUserId or d.departmentId = :departmentId)
                        """;

        /**
         * Bộ lọc của người dùng ({@code ApplicationSearch}); đặt sau
         * {@link #SCOPE_FILTER}.
         */
        String FORMAL_SCOPE_FILTER = SCOPE_FILTER + " and " + FORMAL_FILTER + " ";

        String SEARCH_FILTER = """
                          and (:jobPostingId is null or jp.jobPostingId = :jobPostingId)
                          and (:status is null or a.applicationStatus = :status)
                          and (:keyword is null
                               or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                               or lower(u.email) like lower(concat('%', :keyword, '%')))
                        """;

        @Query("SELECT a FROM Application a " +
                        "LEFT JOIN FETCH a.candidate c " +
                        "LEFT JOIN FETCH c.account " +
                        "LEFT JOIN FETCH a.jobPosting")
        List<Application> findAllWithCandidateAndJobPosting();

        boolean existsByCandidate_CandidateIdAndJobPosting_JobPostingId(Integer candidateId, Integer jobPostingId);

        boolean existsByJobPosting_JobPostingId(Integer jobPostingId);

        /**
         * Chỉ chuyển {@code Applied → AI_Screened}; đơn HR đã xử lý thì giữ nguyên.
         * Câu update viết tay không chạy {@code @PreUpdate} nên phải tự gán
         * {@code updatedAt}.
         *
         * @return 1 nếu đã chuyển, 0 nếu đơn không còn ở {@code Applied}
         */
        @Modifying
        @Query("UPDATE Application a SET a.applicationStatus = 'AI_Screened', a.updatedAt = CURRENT_TIMESTAMP " +
                        "WHERE a.applicationId = :id AND a.applicationStatus = 'Applied'")
        int markScreened(@Param("id") Integer applicationId);

        /**
         * Đổi trạng thái khi duyệt, chỉ khi đơn còn ở một trong các trạng thái
         * {@code from}
         * (người khác vừa xử lý thì không đè). Tự gán {@code updatedAt} vì câu update
         * viết tay không chạy {@code @PreUpdate}.
         *
         * @return 1 nếu đã đổi, 0 nếu đơn không còn ở trạng thái mong đợi
         */
        @Modifying
        @Query("UPDATE Application a SET a.applicationStatus = :to, a.updatedAt = CURRENT_TIMESTAMP " +
                        "WHERE a.applicationId = :id AND a.applicationStatus IN :from")
        int transition(@Param("id") Integer applicationId, @Param("from") Collection<String> from,
                        @Param("to") String to);

    /** Lần AI chấm mới nhất của đơn (mỗi đơn tối đa một dòng); đặt sau {@link #PIPELINE_JOINS}. */
    String LATEST_SCORE_JOIN = """
                left join AIScreeningResult ai on ai.application = a and ai.aiScreeningId = (
                    select max(ai2.aiScreeningId) from AIScreeningResult ai2 where ai2.application = a)
            """;

    /**
     * Id các đơn của danh sách hồ sơ (5.1.22) trong phạm vi người xem và bộ lọc, đúng thứ tự hiển thị:
     * {@code sort = 'score'}: điểm AI cao trước, chưa có điểm xếp cuối; {@code 'newest'}: chỉ theo ngày nộp.
     * Chỉ lấy id: giai đoạn của đơn tính ở Service (xem {@link #findProgressFacts}), rồi mới lọc và cắt trang.
     */
    @Query("select a.applicationId " + PIPELINE_JOINS + LATEST_SCORE_JOIN + FORMAL_SCOPE_FILTER + SEARCH_FILTER + """
            order by case when :sort = 'score' then ai.aiMatchScore end desc nulls last,
                     a.submissionDate desc, a.applicationId desc
            """)
    List<Integer> findPipelineIds(@Param("allDepartments") boolean allDepartments,
                                  @Param("managerUserId") Integer managerUserId,
                                  @Param("departmentId") Integer departmentId,
                                  @Param("jobPostingId") Integer jobPostingId,
                                  @Param("status") String status,
                                  @Param("keyword") String keyword,
                                  @Param("sort") String sort);

    /**
     * Các dòng của một trang danh sách, kèm điểm AI mới nhất; không theo thứ tự nào (Service xếp lại theo
     * {@link #findPipelineIds}). {@code ids} là id của một trang, không được rỗng.
     */
    @Query("""
            select new com.group2.rms.candidate.pipeline.dto.ApplicationPipelineResponse(
                a.applicationId, u.fullName, u.email, jp.postingTitle, a.submissionDate, a.applicationStatus,
                ai.aiMatchScore, c.isPotential)
            """ + PIPELINE_JOINS + LATEST_SCORE_JOIN + "where a.applicationId in :ids")
    List<ApplicationPipelineResponse> findPipelineRows(@Param("ids") Collection<Integer> applicationIds);

        /** Các tin có đơn trong phạm vi người xem, cho ô lọc "Vị trí". */
        @Query("select distinct new com.group2.rms.candidate.pipeline.dto.JobPostingOption(jp.jobPostingId, jp.postingTitle) "
                        + PIPELINE_JOINS + FORMAL_SCOPE_FILTER + "order by jp.postingTitle")
        List<JobPostingOption> findPostingOptions(@Param("allDepartments") boolean allDepartments,
                        @Param("managerUserId") Integer managerUserId,
                        @Param("departmentId") Integer departmentId);

        /**
         * Đơn kèm mọi thứ trang chi tiết và kiểm quyền cần (ứng viên, tin, requisition, phòng ban, trưởng phòng).
         */
        @Query("""
                        select a from Application a
                            join fetch a.candidate c
                            join fetch c.account
                            join fetch a.jobPosting jp
                            join fetch jp.requisition r
                            left join fetch r.department d
                            left join fetch d.manager
                        where a.applicationId = :id
                        """)
        Optional<Application> findDetailById(@Param("id") Integer applicationId);

        /** Đơn của một ứng viên (My Applications), mới nhất trước. */
        @Query("""
                        SELECT a FROM Application a
                            JOIN FETCH a.jobPosting
                        WHERE a.candidate.account.userId = :userId
                        """ + " AND " + FORMAL_FILTER + """
                        ORDER BY a.submissionDate DESC, a.applicationId DESC
                        """)
        List<Application> findOwnedByUserId(@Param("userId") Integer userId);

        /** Một đơn, chỉ khi thuộc về ứng viên {@code userId}. */
        @Query("""
                        SELECT a FROM Application a
                            JOIN FETCH a.jobPosting
                        WHERE a.applicationId = :id
                          AND a.candidate.account.userId = :userId
                        """ + " AND " + FORMAL_FILTER + """
                        """)
        Optional<Application> findOwnedById(@Param("id") Integer applicationId, @Param("userId") Integer userId);

        /**
         * Ghi dấu ứng viên tự rút đơn. Schema không có trạng thái {@code Withdrawn}:
         * đơn chuyển {@code Rejected},
         * dòng {@code AuditLog} này phân biệt "đã rút" với "bị loại". SQL thuần vì
         * module admin (chủ bảng AuditLog)
         * phụ thuộc user, user phụ thuộc candidate: import entity của admin vào đây sẽ
         * thành vòng phụ thuộc.
         */
        @Modifying
        @Query(value = "INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue) "
                        + "VALUES (:userId, 'UPDATE', 'Application', CAST(:id AS NVARCHAR(50)), :oldStatus, 'Withdrawn')", nativeQuery = true)
        void recordWithdrawal(@Param("userId") Integer userId, @Param("id") Integer applicationId,
                        @Param("oldStatus") String oldStatus);

        /**
         * Các đơn ứng viên {@code userId} đã tự rút (dấu do {@link #recordWithdrawal}
         * ghi).
         */
        @Query(value = "SELECT CAST(l.EntityId AS INT) FROM AuditLog l "
                        + "WHERE l.EntityName = 'Application' AND l.NewValue = 'Withdrawn' AND l.UserId = :userId", nativeQuery = true)
        List<Integer> findWithdrawnApplicationIds(@Param("userId") Integer userId);

        /**
         * Lịch phỏng vấn của đơn, cho timeline. SQL thuần vì module interview đã phụ
         * thuộc candidate:
         * import entity của interview vào đây sẽ thành vòng phụ thuộc.
         */
        @Query(value = "SELECT s.StartTime AS at, s.InterviewStatus AS status FROM InterviewSchedule s "
                        + "WHERE s.ApplicationId = :id", nativeQuery = true)
        List<TimelineEventRow> findInterviewEvents(@Param("id") Integer applicationId);

        /**
         * Kết quả chốt các buổi phỏng vấn của đơn ({@code Passed}/{@code Failed}), cho
         * timeline. SQL thuần, cùng lý do.
         */
        @Query(value = "SELECT r.ApprovedAt AS at, r.FinalDecision AS status FROM InterviewFinalResult r "
                        + "JOIN InterviewSchedule s ON s.InterviewId = r.InterviewId "
                        + "WHERE s.ApplicationId = :id", nativeQuery = true)
        List<TimelineEventRow> findInterviewResultEvents(@Param("id") Integer applicationId);

        /**
         * Offer của đơn, cho timeline. SQL thuần, cùng lý do với
         * {@link #findInterviewEvents}.
         */
        @Query(value = "SELECT o.CreatedAt AS at, o.OfferStatus AS status FROM OfferProposal o "
                        + "WHERE o.ApplicationId = :id", nativeQuery = true)
        List<TimelineEventRow> findOfferEvents(@Param("id") Integer applicationId);

    /**
     * Dữ kiện tính trạng thái chi tiết cho nhiều đơn trong một câu (cả trang danh sách), xem
     * {@code ApplicationProgressFacts}. Lịch mới nhất: {@code StartTime} lớn nhất, bỏ lịch đã hủy.
     * SQL thuần, cùng lý do với {@link #findInterviewEvents}. {@code ids} không được rỗng, tối đa ~2000
     * (giới hạn tham số của SQL Server): danh sách dài hơn thì gọi theo lô.
     */
    @Query(value = """
            SELECT a.ApplicationId AS applicationId,
                   a.ApplicationStatus AS applicationStatus,
                   CAST(CASE WHEN EXISTS (
                            SELECT 1 FROM AuditLog l
                            WHERE l.EntityName = 'Application' AND l.NewValue = 'Withdrawn'
                              AND l.EntityId = CAST(a.ApplicationId AS NVARCHAR(50)))
                        THEN 1 ELSE 0 END AS BIT) AS withdrawn,
                   s.InterviewId AS interviewId,
                   s.InterviewStatus AS interviewStatus,
                   s.StartTime AS interviewStart,
                   (SELECT COUNT(*) FROM InterviewPanel p WHERE p.InterviewId = s.InterviewId) AS panelCount,
                   (SELECT COUNT(DISTINCT e.InterviewerId) FROM InterviewEvaluation e
                    WHERE e.InterviewId = s.InterviewId) AS evaluationCount,
                   r.FinalDecision AS finalDecision,
                   o.OfferStatus AS offerStatus
            FROM Application a
            OUTER APPLY (SELECT TOP 1 s1.InterviewId, s1.InterviewStatus, s1.StartTime
                         FROM InterviewSchedule s1
                         WHERE s1.ApplicationId = a.ApplicationId AND s1.InterviewStatus <> 'Cancelled'
                         ORDER BY s1.StartTime DESC, s1.InterviewId DESC) s
            LEFT JOIN InterviewFinalResult r ON r.InterviewId = s.InterviewId
            OUTER APPLY (SELECT TOP 1 o1.OfferStatus
                         FROM OfferProposal o1
                         WHERE o1.ApplicationId = a.ApplicationId
                         ORDER BY o1.OfferId DESC) o
            WHERE a.ApplicationId IN (:ids)
            """, nativeQuery = true)
    List<ProgressFactsRow> findProgressFacts(@Param("ids") Collection<Integer> applicationIds);

        // new query
        // update condition duplicate application with HM SUGGEST
        @Query("""
                        Select Case when count(a) >0 then true else false end
                        from Application a
                        where a.candidate.candidateId = :candidateId
                        AND a.jobPosting.jobPostingId = :jobPostingId
                        """ + " AND " + FORMAL_FILTER)
        boolean existsActiveByCandidateAndJobPosting(@Param("candidateId") Integer candidateId,
                        @Param("jobPostingId") Integer jobPostingId);

        // query get list candidate recommend by HM
        @Query("""
                        SELECT a FROM Application a
                            JOIN FETCH a.candidate c
                            JOIN FETCH c.account
                        WHERE a.jobPosting.jobPostingId = :jobPostingId
                          AND a.applicationStatus = 'HM_Suggested'
                        """ + " and not (" + PENDING_HR_RECOMMENDATION + ") order by a.submissionDate desc, a.applicationId desc")
        List<Application> findSuggestionsByJobPostingId(@Param("jobPostingId") Integer jobPostingId);

        @Query("select a from Application a where a.jobPosting.jobPostingId = :id and a.applicationStatus = 'HM_Suggested'")
        List<Application> findAllDraftsByJobPostingId(@Param("id") Integer jobPostingId);

        @Query("select a from Application a join fetch a.candidate c join fetch c.account where a.jobPosting.jobPostingId = :id and "
                + HR_RECOMMENDATION + """
                  and not exists (select l.auditLogId from AuditLog l where l.entityName = 'Application'
                    and l.entityId = cast(a.applicationId as string) and l.newValue = 'TalentPoolHrWithdrawn')
                  order by a.createdAt desc, a.applicationId desc
                """)
        List<Application> findHrRecommendations(@Param("id") Integer jobPostingId);

        @Query("select count(a) from Application a where a.applicationId = :id and a.applicationStatus = 'HM_Suggested' and " + PENDING_HR_RECOMMENDATION)
        long countPendingHrRecommendation(@Param("id") Integer applicationId);

        @Query("select count(a) from Application a where a.jobPosting.jobPostingId = :id and a.applicationStatus = 'HM_Suggested' and " + PENDING_HR_RECOMMENDATION)
        long countPendingHrRecommendationsForJob(@Param("id") Integer jobPostingId);

        // query get candidate CV
        @Query("""
                        SELECT a FROM Application a
                        WHERE a.candidate.candidateId = :candidateId
                          AND a.appliedCvUrl IS NOT NULL
                          AND a.appliedCvUrl <> ''
                        """ + " AND " + FORMAL_FILTER + """
                        ORDER BY a.submissionDate DESC
                        """)
        List<Application> findPreviousCvByCandidateId(@Param("candidateId") Integer candidateId);

        // Update status to HM PASSED
        @Modifying
        @Query("""
                        UPDATE Application a
                        SET a.applicationStatus = 'HM_Passed',
                            a.updatedAt = CURRENT_TIMESTAMP,
                            a.submissionDate = CURRENT_TIMESTAMP
                        WHERE a.applicationId = :id
                          AND a.applicationStatus = 'HM_Suggested'
                        """)
        int promoteSuggestionToHmPassed(@Param("id") Integer applicationId);

        // Candidate uploads a new CV: approved proposal goes straight to interview;
        // an HR recommendation still awaiting HM follows ordinary application screening.
        @Modifying
        @Query("""
                        UPDATE Application a
                        SET a.applicationStatus = :status,
                        a.appliedCvUrl = :cv,
                        a.submissionDate = CURRENT_TIMESTAMP,
                        a.updatedAt = CURRENT_TIMESTAMP
                        WHERE a.applicationId = :id
                        AND a.applicationStatus = 'HM_Suggested'
                        """ + " AND :status IN ('Applied', 'HM_Passed') AND (:status = 'Applied' OR NOT (" + PENDING_HR_RECOMMENDATION + "))")
        int submitSuggestionWithNewCv(@Param("id") Integer id, @Param("cv") String cv, @Param("status") String status);

        // Update status from HM_Suggested to CANDIDATE_SUBMITTED
        @Modifying
        @Query(value = "INSERT INTO AuditLog(UserId,[Action],EntityName,EntityId,OldValue,NewValue) VALUES(:userId,'UPDATE','Application',CAST(:id AS NVARCHAR(50)),'HM_Suggested','TalentPoolCandidateSubmitted')", nativeQuery = true)
        void recordSuggestionSubmitted(@Param("userId") Integer userId, @Param("id") Integer id);

        // get application process when close job posting
        @Query("""
                        SELECT a FROM Application a
                        WHERE a.jobPosting.jobPostingId = :jobPostingId
                          AND a.applicationStatus NOT IN ('Hired', 'Rejected', 'HM_Suggested')
                        """)
        List<Application> findPendingApplicationsForClosedJob(@Param("jobPostingId") Integer jobPostingId);

        Optional<Application> findFirstByCandidate_CandidateIdAndJobPosting_JobPostingIdAndApplicationStatus(
                        Integer candidateId, Integer jobPostingId, String applicationStatus);

        @Query("select a from Application a join fetch a.candidate c join fetch c.account join fetch a.jobPosting jp where a.applicationStatus in ('HM_Passed','Interviewing') and jp.postingStatus = 'Published'")
        List<Application> findEligibleForInterview();

        @Query(value = "select count(*) from AuditLog where EntityName = 'Application' and EntityId = cast(:id as nvarchar(50)) and NewValue = 'TalentPoolCancelled'", nativeQuery = true)
        int countTalentPoolCancellation(@Param("id") Integer applicationId);

        @Query(value = """
                select l.[Timestamp] as at, l.NewValue as kind, u.FullName as actor
                from AuditLog l join [User] u on u.UserId = l.UserId
                where l.EntityName = 'Application' and l.EntityId = cast(:id as nvarchar(50))
                  and l.NewValue in ('TalentPoolSuggested','TalentPoolHrRecommended','TalentPoolActivated','TalentPoolCandidateSubmitted','TalentPoolCancelled')
                order by l.[Timestamp] desc, l.AuditLogId desc
                """, nativeQuery = true)
        List<TalentPoolEventRow> findTalentPoolEvents(@Param("id") Integer applicationId);

        interface TalentPoolEventRow {
                LocalDateTime getAt();
                String getKind();
                String getActor();
        }

        @Query(value = "select count(*) from AuditLog where EntityName = 'Application' and EntityId = cast(:id as nvarchar(50)) and NewValue = 'PostingClosed'", nativeQuery = true)
        int countPostingClosure(@Param("id") Integer applicationId);

        /**
         * Một dòng của {@link #findProgressFacts}; các trường lịch/offer {@code null}
         * nếu chưa có.
         */
        interface ProgressFactsRow {
                Integer getApplicationId();

                String getApplicationStatus();

                Boolean getWithdrawn();

                Long getInterviewId();

                String getInterviewStatus();

                LocalDateTime getInterviewStart();

                Integer getPanelCount();

                Integer getEvaluationCount();

                String getFinalDecision();

                String getOfferStatus();
        }

        /** Một mốc đọc từ bảng của module khác: thời điểm + trạng thái gốc. */
        interface TimelineEventRow {
                LocalDateTime getAt();

                String getStatus();
        }

        // dunglt bổ sung: 3 câu dữ kiện cho My Applications (5.1.38), mỗi câu lấy cho mọi đơn của
        // một ứng viên (theo userId) nên số câu không tăng theo số đơn. SQL thuần, cùng lý do với
        // findInterviewEvents.

        /** Mọi lịch phỏng vấn (kể cả lịch đã hủy) của các đơn thuộc ứng viên {@code userId}, cũ nhất trước. */
        @Query(value = """
                        SELECT s.ApplicationId AS applicationId, s.StartTime AS startTime,
                               s.InterviewFormat AS format, s.InterviewStatus AS status
                        FROM InterviewSchedule s
                            JOIN Application a ON a.ApplicationId = s.ApplicationId
                            JOIN Candidate c ON c.CandidateId = a.CandidateId
                        WHERE c.UserId = :userId
                        ORDER BY s.StartTime, s.InterviewId
                        """, nativeQuery = true)
        List<MyInterviewRow> findMyInterviews(@Param("userId") Integer userId);

        /**
         * Offer ứng viên {@code userId} được thấy: đã gửi cho họ hoặc họ đã trả lời. Offer nội bộ
         * ({@code Draft}, {@code Pending_Director}, {@code Director_*}) và offer đã xóa mềm bị lọc ngay
         * trong câu SQL (GBR-02). {@code UpdatedAt} trống thì lấy {@code CreatedAt}.
         */
        @Query(value = """
                        SELECT o.ApplicationId AS applicationId, o.OfferId AS offerId, o.OfferStatus AS status,
                               o.ResponseDeadline AS responseDeadline, COALESCE(o.UpdatedAt, o.CreatedAt) AS at
                        FROM OfferProposal o
                            JOIN Application a ON a.ApplicationId = o.ApplicationId
                            JOIN Candidate c ON c.CandidateId = a.CandidateId
                        WHERE c.UserId = :userId
                          AND o.IsDeleted = 0
                          AND o.OfferStatus IN ('Sent_Candidate', 'Accepted', 'Declined')
                        """, nativeQuery = true)
        List<MyOfferRow> findMyVisibleOffers(@Param("userId") Integer userId);

        /**
         * Dấu {@code Withdrawn} (ứng viên tự rút) và {@code PostingClosed} (dừng do đóng tin) trên các đơn
         * của ứng viên {@code userId}, cũ nhất trước. Lọc theo chủ đơn, không theo người ghi dấu: dấu
         * {@code PostingClosed} do HR ghi. {@code OldValue} là trạng thái đơn ngay trước khi dừng.
         */
        @Query(value = """
                        SELECT a.ApplicationId AS applicationId, l.NewValue AS kind,
                               l.OldValue AS oldStatus, l.[Timestamp] AS at
                        FROM AuditLog l
                            JOIN Application a ON l.EntityName = 'Application'
                                AND l.EntityId = CAST(a.ApplicationId AS NVARCHAR(50))
                            JOIN Candidate c ON c.CandidateId = a.CandidateId
                        WHERE c.UserId = :userId
                          AND l.NewValue IN ('Withdrawn', 'PostingClosed')
                        ORDER BY l.[Timestamp], l.AuditLogId
                        """, nativeQuery = true)
        List<MyApplicationMarkRow> findMyApplicationMarks(@Param("userId") Integer userId);

        /** Một dòng của {@link #findMyInterviews}. */
        interface MyInterviewRow {
                Integer getApplicationId();

                LocalDateTime getStartTime();

                String getFormat();

                String getStatus();
        }

        /** Một dòng của {@link #findMyVisibleOffers}; {@code responseDeadline} có thể {@code null}. */
        interface MyOfferRow {
                Integer getApplicationId();

                Integer getOfferId();

                String getStatus();

                LocalDateTime getResponseDeadline();

                LocalDateTime getAt();
        }

        /** Một dòng của {@link #findMyApplicationMarks}; {@code oldStatus} có thể {@code null}. */
        interface MyApplicationMarkRow {
                Integer getApplicationId();

                String getKind();

                String getOldStatus();

                LocalDateTime getAt();
        }
}
