package com.group2.rms.candidate.domain.repository;

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

        @Query("SELECT a FROM Application a " +
                        "LEFT JOIN FETCH a.candidate c " +
                        "LEFT JOIN FETCH c.account " +
                        "LEFT JOIN FETCH a.jobPosting")
        List<Application> findAllWithCandidateAndJobPosting();

        boolean existsByCandidate_CandidateIdAndJobPosting_JobPostingId(Integer candidateId, Integer jobPostingId);

        List<Application> findByCandidate_CandidateIdOrderByApplicationIdDesc(Integer candidateId);

        boolean existsByJobPosting_JobPostingId(Integer jobPostingId);

        /**
         * Số đơn chính thức của tin đã qua vòng CV: HR từng cho Đạt (kể cả đơn bị loại ở bước sau), hoặc đang ở
         * bước sau vòng CV (đơn đi đường talent pool lên thẳng {@code HM_Passed}, không có lượt HR).
         */
        @Query("SELECT COUNT(a) FROM Application a WHERE a.jobPosting.jobPostingId = :jobPostingId AND "
                        + FORMAL_FILTER + """
                         AND (a.applicationStatus IN ('HR_Passed', 'HM_Passed', 'Interviewing', 'Offered', 'Hired')
                              OR EXISTS (SELECT r.reviewId FROM ApplicationReview r WHERE r.application = a
                                         AND r.reviewerRole = 'HR' AND r.decision = 'Pass'))
                        """)
        long countPastCvRoundByJobPosting(@Param("jobPostingId") Integer jobPostingId);

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
     * Dữ kiện tính trạng thái chi tiết cho nhiều đơn trong một câu (cả trang danh sách), xem
     * {@code ApplicationProgressFacts}. Lịch mới nhất: {@code StartTime} lớn nhất, bỏ lịch đã hủy.
     * SQL thuần, cùng lý do với {@code ApplicationPipelineRepository.findInterviewEvents}. {@code ids} không được rỗng, tối đa ~2000
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

        /** BR-CAN-01: đã nộp vào bất kỳ tin nào của cùng đợt (Requisition) với tin này thì coi như đã ứng tuyển. */
        @Query("""
                        select case when count(a) > 0 then true else false end
                        from Application a
                        where a.candidate.candidateId = :candidateId
                        and a.jobPosting.requisition.requisitionId = (select jp.requisition.requisitionId
                            from JobPosting jp where jp.jobPostingId = :jobPostingId)
                        """ + " and " + FORMAL_FILTER)
        boolean existsActiveByCandidateInSameRequisition(@Param("candidateId") Integer candidateId,
                        @Param("jobPostingId") Integer jobPostingId);

        @Query("select count(a) from Application a where a.applicationId = :id and a.applicationStatus = 'HM_Suggested' and " + PENDING_HR_RECOMMENDATION)
        long countPendingHrRecommendation(@Param("id") Integer applicationId);

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

        // Candidate uploads a new CV: every proposal enters ordinary application screening.
        // Only the separate HR old-CV confirmation may promote a proposal to HM_Passed.
        @Modifying
        @Query("""
                        UPDATE Application a
                        SET a.applicationStatus = :status,
                        a.appliedCvUrl = :cv,
                        a.submissionDate = CURRENT_TIMESTAMP,
                        a.updatedAt = CURRENT_TIMESTAMP
                        WHERE a.applicationId = :id
                        AND a.applicationStatus = 'HM_Suggested'
                        """ + " AND :status = 'Applied'")
        int submitSuggestionWithNewCv(@Param("id") Integer id, @Param("cv") String cv, @Param("status") String status);

        // Update status from HM_Suggested to CANDIDATE_SUBMITTED
        @Modifying
        @Query(value = "INSERT INTO AuditLog(UserId,[Action],EntityName,EntityId,OldValue,NewValue) VALUES(:userId,'UPDATE','Application',CAST(:id AS NVARCHAR(50)),'HM_Suggested','TalentPoolCandidateSubmitted')", nativeQuery = true)
        void recordSuggestionSubmitted(@Param("userId") Integer userId, @Param("id") Integer id);

        Optional<Application> findFirstByCandidate_CandidateIdAndJobPosting_JobPostingIdAndApplicationStatus(
                        Integer candidateId, Integer jobPostingId, String applicationStatus);

        @Query("select a from Application a join fetch a.candidate c join fetch c.account join fetch a.jobPosting jp where a.applicationStatus in ('HM_Passed','Interviewing') and jp.postingStatus = 'Published'")
        List<Application> findEligibleForInterview();

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
}
