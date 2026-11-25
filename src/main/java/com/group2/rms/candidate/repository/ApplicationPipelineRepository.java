package com.group2.rms.candidate.repository;

import com.group2.rms.candidate.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.dto.JobPostingOption;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.core.util.ListFilters;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static com.group2.rms.candidate.repository.ApplicationRepository.FORMAL_SCOPE_FILTER;
import static com.group2.rms.candidate.repository.ApplicationRepository.PIPELINE_JOINS;

/**
 * Repository truy vấn danh sách và các sự kiện liên quan đến hồ sơ ứng tuyển.
 */
@org.springframework.stereotype.Repository
public interface ApplicationPipelineRepository extends Repository<Application, Integer> {

        String SEARCH_FILTER = """
                        and (:jobPostingId is null or jp.jobPostingId = :jobPostingId)
                        and (:status is null or a.applicationStatus = :status)
                        and (:keywordPattern is null
                             or collate(u.fullName as Latin1_General_100_CI_AI) like :keywordPattern escape '\\'
                             or collate(u.email as Latin1_General_100_CI_AI) like :keywordPattern escape '\\')
                        """;

        /**
         * Lấy danh sách ID các hồ sơ ứng tuyển phù hợp bộ lọc và phân quyền.
         */
        default List<Integer> findPipelineIds(
                        boolean allDepartments, Integer managerUserId, Integer departmentId,
                        Integer jobPostingId, String status, String keyword) {
                String pattern = keyword == null || keyword.isBlank()
                                ? null
                                : "%" + ListFilters.escapeLike(keyword.strip()) + "%";
                return findPipelineIdsMatching(allDepartments, managerUserId, departmentId, jobPostingId, status,
                                pattern);
        }

        @Query("select a.applicationId " + PIPELINE_JOINS + FORMAL_SCOPE_FILTER + SEARCH_FILTER + """
                        order by a.submissionDate desc, a.applicationId desc
                        """)
        List<Integer> findPipelineIdsMatching(
                        @Param("allDepartments") boolean allDepartments,
                        @Param("managerUserId") Integer managerUserId,
                        @Param("departmentId") Integer departmentId,
                        @Param("jobPostingId") Integer jobPostingId,
                        @Param("status") String status,
                        @Param("keywordPattern") String keywordPattern);

        @Query("""
                        select new com.group2.rms.candidate.dto.ApplicationPipelineResponse(
                            a.applicationId, u.fullName, u.email, jp.postingTitle, a.submissionDate, a.applicationStatus,
                            c.isPotential)
                        """
                        + PIPELINE_JOINS + "where a.applicationId in :ids")
        List<ApplicationPipelineResponse> findPipelineRows(@Param("ids") Collection<Integer> applicationIds);

        @Query("""
                        select distinct new com.group2.rms.candidate.dto.JobPostingOption(jp.jobPostingId, jp.postingTitle)
                        """
                        + PIPELINE_JOINS + FORMAL_SCOPE_FILTER + """
                                        order by jp.postingTitle
                                        """)
        List<JobPostingOption> findPostingOptions(
                        @Param("allDepartments") boolean allDepartments,
                        @Param("managerUserId") Integer managerUserId,
                        @Param("departmentId") Integer departmentId);

        @Query(value = """
                        SELECT s.StartTime AS at, s.InterviewStatus AS status
                        FROM InterviewSchedule s
                        WHERE s.ApplicationId = :id
                        """, nativeQuery = true)
        List<TimelineEventRow> findInterviewEvents(@Param("id") Integer applicationId);

        @Query(value = """
                        SELECT r.ApprovedAt AS at, r.FinalDecision AS status
                        FROM InterviewFinalResult r
                        JOIN InterviewSchedule s ON s.InterviewId = r.InterviewId
                        WHERE s.ApplicationId = :id
                        """, nativeQuery = true)
        List<TimelineEventRow> findInterviewResultEvents(@Param("id") Integer applicationId);

        @Query(value = """
                        SELECT o.CreatedAt AS at, o.OfferStatus AS status
                        FROM OfferProposal o
                        WHERE o.ApplicationId = :id
                        """, nativeQuery = true)
        List<TimelineEventRow> findOfferEvents(@Param("id") Integer applicationId);

        @Query(value = """
                        SELECT COUNT(*) FROM AuditLog
                        WHERE EntityName = 'Application'
                          AND EntityId = CAST(:id AS NVARCHAR(50))
                          AND NewValue = 'TalentPoolCancelled'
                        """, nativeQuery = true)
        int countTalentPoolCancellation(@Param("id") Integer applicationId);

        @Query(value = """
                        SELECT l.[Timestamp] AS at, l.NewValue AS kind, u.FullName AS actor
                        FROM AuditLog l
                        JOIN [User] u ON u.UserId = l.UserId
                        WHERE l.EntityName = 'Application'
                          AND l.EntityId = CAST(:id AS NVARCHAR(50))
                          AND l.NewValue IN ('TalentPoolSuggested', 'TalentPoolHrRecommended',
                                             'TalentPoolActivated', 'TalentPoolCandidateSubmitted', 'TalentPoolCancelled')
                        ORDER BY l.[Timestamp] DESC, l.AuditLogId DESC
                        """, nativeQuery = true)
        List<TalentPoolEventRow> findTalentPoolEvents(@Param("id") Integer applicationId);

        /** Các lần khôi phục hồ sơ bị loại ở vòng CV, cho timeline (NewValue bắt đầu bằng {@code RestoredBy}). */
        @Query(value = """
                        SELECT l.[Timestamp] AS at, l.NewValue AS kind, u.FullName AS actor
                        FROM AuditLog l
                        JOIN [User] u ON u.UserId = l.UserId
                        WHERE l.EntityName = 'Application'
                          AND l.EntityId = CAST(:id AS NVARCHAR(50))
                          AND l.NewValue LIKE 'RestoredBy%'
                        ORDER BY l.[Timestamp] DESC, l.AuditLogId DESC
                        """, nativeQuery = true)
        List<TalentPoolEventRow> findRestorationEvents(@Param("id") Integer applicationId);

        interface TalentPoolEventRow {
                LocalDateTime getAt();

                String getKind();

                String getActor();
        }

        interface TimelineEventRow {
                LocalDateTime getAt();

                String getStatus();
        }
}
