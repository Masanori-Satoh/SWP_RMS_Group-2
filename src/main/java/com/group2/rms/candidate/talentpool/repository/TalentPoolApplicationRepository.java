package com.group2.rms.candidate.talentpool.repository;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.group2.rms.candidate.domain.repository.ApplicationRepository.FORMAL_FILTER;
import static com.group2.rms.candidate.domain.repository.ApplicationRepository.HR_RECOMMENDATION;
import static com.group2.rms.candidate.domain.repository.ApplicationRepository.PENDING_HR_RECOMMENDATION;

/**
 * Câu đọc riêng của Talent Pool trên bảng Application. Câu ghi và câu đọc dùng chung nằm ở
 * {@link ApplicationRepository}.
 */
@Repository
public interface TalentPoolApplicationRepository extends org.springframework.data.repository.Repository<Application, Integer> {
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

        // get application process when close job posting
        @Query("""
                        SELECT a FROM Application a
                        WHERE a.jobPosting.jobPostingId = :jobPostingId
                          AND a.applicationStatus NOT IN :excluded
                        """)
        List<Application> findPendingApplicationsForClosedJob(@Param("jobPostingId") Integer jobPostingId,
                        @Param("excluded") java.util.Collection<String> excluded);

        // dunglt bổ sung: bỏ qua mọi đơn đã kết thúc (ApplicationStatusService.TERMINAL) và bản nháp đề xuất
        default List<Application> findPendingApplicationsForClosedJob(Integer jobPostingId) {
                java.util.Set<String> excluded = new java.util.HashSet<>(
                                com.group2.rms.candidate.domain.rule.ApplicationStatusService.TERMINAL);
                excluded.add("HM_Suggested");
                return findPendingApplicationsForClosedJob(jobPostingId, excluded);
        }
}
