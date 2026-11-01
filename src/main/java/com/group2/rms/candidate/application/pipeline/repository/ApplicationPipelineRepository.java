package com.group2.rms.candidate.application.pipeline.repository;

import com.group2.rms.candidate.application.pipeline.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.application.pipeline.dto.JobPostingOption;
import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static com.group2.rms.candidate.domain.repository.ApplicationRepository.FORMAL_SCOPE_FILTER;
import static com.group2.rms.candidate.domain.repository.ApplicationRepository.PIPELINE_JOINS;

/**
 * Câu đọc riêng của danh sách và trang chi tiết hồ sơ (5.1.22). Câu ghi và câu đọc dùng chung nằm ở
 * {@link ApplicationRepository}.
 */
@Repository
public interface ApplicationPipelineRepository extends org.springframework.data.repository.Repository<Application, Integer> {
        String SEARCH_FILTER = """
                          and (:jobPostingId is null or jp.jobPostingId = :jobPostingId)
                          and (:status is null or a.applicationStatus = :status)
                          and (:keyword is null
                               or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                               or lower(u.email) like lower(concat('%', :keyword, '%')))
                        """;

    /**
     * Id các đơn của danh sách hồ sơ (5.1.22) trong phạm vi người xem và bộ lọc, đúng thứ tự hiển thị:
     * mới nộp trước.
     * Chỉ lấy id: giai đoạn của đơn tính ở Service (xem {@link ApplicationRepository#findProgressFacts}), rồi mới lọc và cắt trang.
     */
    @Query("select a.applicationId " + PIPELINE_JOINS + FORMAL_SCOPE_FILTER + SEARCH_FILTER + """
            order by a.submissionDate desc, a.applicationId desc
            """)
    List<Integer> findPipelineIds(@Param("allDepartments") boolean allDepartments,
                                  @Param("managerUserId") Integer managerUserId,
                                  @Param("departmentId") Integer departmentId,
                                  @Param("jobPostingId") Integer jobPostingId,
                                  @Param("status") String status,
                                  @Param("keyword") String keyword);

    /**
     * Các dòng của một trang danh sách; không theo thứ tự nào (Service xếp lại theo
     * {@link #findPipelineIds}). {@code ids} là id của một trang, không được rỗng.
     */
    @Query("""
            select new com.group2.rms.candidate.application.pipeline.dto.ApplicationPipelineResponse(
                a.applicationId, u.fullName, u.email, jp.postingTitle, a.submissionDate, a.applicationStatus,
                c.isPotential)
            """ + PIPELINE_JOINS + "where a.applicationId in :ids")
    List<ApplicationPipelineResponse> findPipelineRows(@Param("ids") Collection<Integer> applicationIds);

        /** Các tin có đơn trong phạm vi người xem, cho ô lọc "Vị trí". */
        @Query("select distinct new com.group2.rms.candidate.application.pipeline.dto.JobPostingOption(jp.jobPostingId, jp.postingTitle) "
                        + PIPELINE_JOINS + FORMAL_SCOPE_FILTER + "order by jp.postingTitle")
        List<JobPostingOption> findPostingOptions(@Param("allDepartments") boolean allDepartments,
                        @Param("managerUserId") Integer managerUserId,
                        @Param("departmentId") Integer departmentId);

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

        /** Một mốc đọc từ bảng của module khác: thời điểm + trạng thái gốc. */
        interface TimelineEventRow {
                LocalDateTime getAt();

                String getStatus();
        }
}
