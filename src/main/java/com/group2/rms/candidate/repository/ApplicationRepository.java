package com.group2.rms.candidate.repository;

import com.group2.rms.candidate.entity.Application;
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

        String HR_RECOMMENDATION = """
                        exists (select l.auditLogId from AuditLog l where l.entityName = 'Application'
                            and l.entityId = cast(a.applicationId as string) and l.newValue = 'TalentPoolHrRecommended')
                        """;

        String PENDING_HR_RECOMMENDATION = HR_RECOMMENDATION + """
                        and exists (select rv.reviewId from ApplicationReview rv where rv.application = a
                            and rv.reviewerRole = 'HR' and rv.decision = 'Hold')
                        and not exists (select rv.reviewId from ApplicationReview rv where rv.application = a
                            and rv.reviewerRole = 'HiringManager' and rv.decision in ('Pass','Fail'))
                        """;

        String FORMAL_FILTER = """
                        a.applicationStatus <> 'HM_Suggested'
                        and not exists (select l.auditLogId from AuditLog l
                            where l.entityName = 'Application' and l.entityId = cast(a.applicationId as string)
                              and l.newValue = 'TalentPoolCancelled')
                        """;

        String PIPELINE_JOINS = """
                        from Application a
                            join a.candidate c
                            join c.account u
                            join a.jobPosting jp
                            join jp.requisition r
                            left join r.department d
                            left join d.manager m
                        """;

        String SCOPE_FILTER = """
                        where (:allDepartments = true or m.userId = :managerUserId or d.departmentId = :departmentId)
                        """;

        String FORMAL_SCOPE_FILTER = SCOPE_FILTER + " and " + FORMAL_FILTER + " ";

        @Query("""
                        SELECT a FROM Application a
                        LEFT JOIN FETCH a.candidate c
                        LEFT JOIN FETCH c.account
                        LEFT JOIN FETCH a.jobPosting
                        """)
        List<Application> findAllWithCandidateAndJobPosting();

        boolean existsByCandidate_CandidateIdAndJobPosting_JobPostingId(Integer candidateId, Integer jobPostingId);

        List<Application> findByCandidate_CandidateIdOrderByApplicationIdDesc(Integer candidateId);

        boolean existsByJobPosting_JobPostingId(Integer jobPostingId);

        /**
         * Chuyển đổi trạng thái đơn ứng tuyển theo điều kiện trạng thái hiện tại.
         */
        @Modifying
        @Query("""
                        UPDATE Application a
                        SET a.applicationStatus = :to, a.updatedAt = CURRENT_TIMESTAMP
                        WHERE a.applicationId = :id AND a.applicationStatus IN :from
                        """)
        int transition(
                        @Param("id") Integer applicationId,
                        @Param("from") Collection<String> from,
                        @Param("to") String to);

        /**
         * Lấy đầy đủ thông tin chi tiết của đơn ứng tuyển kèm các liên kết phụ thuộc.
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
         * Ghi nhận nhật ký kiểm toán khi ứng viên tự rút đơn ứng tuyển.
         */
        @Modifying
        @Query(value = """
                        INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue)
                        VALUES (:userId, 'UPDATE', 'Application', CAST(:id AS NVARCHAR(50)), :oldStatus, 'Withdrawn')
                        """, nativeQuery = true)
        void recordWithdrawal(
                        @Param("userId") Integer userId,
                        @Param("id") Integer applicationId,
                        @Param("oldStatus") String oldStatus);

        /**
         * Truy vấn thông tin dữ kiện tiến trình phỏng vấn và thư mời cho danh sách hồ
         * sơ.
         */
        @Query(value = """
                        SELECT a.ApplicationId AS applicationId,
                               a.ApplicationStatus AS applicationStatus,
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

        @Query("""
                        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
                        FROM Application a
                        WHERE a.candidate.candidateId = :candidateId
                          AND a.jobPosting.jobPostingId = :jobPostingId
                          AND """ + " " + FORMAL_FILTER)
        boolean existsActiveByCandidateAndJobPosting(
                        @Param("candidateId") Integer candidateId,
                        @Param("jobPostingId") Integer jobPostingId);

        @Query("""
                        SELECT COUNT(a) FROM Application a
                        WHERE a.applicationId = :id
                          AND a.applicationStatus = 'HM_Suggested'
                          AND """ + " " + PENDING_HR_RECOMMENDATION)
        long countPendingHrRecommendation(@Param("id") Integer applicationId);

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

        @Modifying
        @Query("""
                        UPDATE Application a
                        SET a.applicationStatus = :status,
                            a.appliedCvUrl = :cv,
                            a.submissionDate = CURRENT_TIMESTAMP,
                            a.updatedAt = CURRENT_TIMESTAMP
                        WHERE a.applicationId = :id
                          AND a.applicationStatus = 'HM_Suggested'
                          AND :status = 'Applied'
                        """)
        int submitSuggestionWithNewCv(
                        @Param("id") Integer id,
                        @Param("cv") String cv,
                        @Param("status") String status);

        @Modifying
        @Query(value = """
                        INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue)
                        VALUES (:userId, 'UPDATE', 'Application', CAST(:id AS NVARCHAR(50)),
                                'HM_Suggested', 'TalentPoolCandidateSubmitted')
                        """, nativeQuery = true)
        void recordSuggestionSubmitted(@Param("userId") Integer userId, @Param("id") Integer id);

        Optional<Application> findFirstByCandidate_CandidateIdAndJobPosting_JobPostingIdAndApplicationStatus(
                        Integer candidateId, Integer jobPostingId, String applicationStatus);

        @Query("""
                        SELECT a FROM Application a
                        JOIN FETCH a.candidate c
                        JOIN FETCH c.account
                        JOIN FETCH a.jobPosting jp
                        WHERE a.applicationStatus IN ('HM_Passed', 'Interviewing')
                          AND jp.postingStatus = 'Published'
                        """)
        List<Application> findEligibleForInterview();

        /**
         * Dòng dữ liệu biểu diễn các sự kiện tiến trình của một đơn ứng tuyển.
         */
        interface ProgressFactsRow {
                Integer getApplicationId();

                String getApplicationStatus();

                Long getInterviewId();

                String getInterviewStatus();

                LocalDateTime getInterviewStart();

                Integer getPanelCount();

                Integer getEvaluationCount();

                String getFinalDecision();

                String getOfferStatus();
        }
}
