package com.group2.rms.candidate.repository;

import com.group2.rms.candidate.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.dto.JobPostingOption;
import com.group2.rms.candidate.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Đơn → ứng viên → tin → requisition → phòng ban (+ trưởng phòng), dùng chung cho các query danh sách. */
    String PIPELINE_JOINS = """
            from Application a
                join a.candidate c
                join c.account u
                join a.jobPosting jp
                join jp.requisition r
                left join r.department d
                left join d.manager m
            """;

    /** Phạm vi người xem ({@code ApplicationScope}): HM chỉ thấy phòng ban mình quản lý và đơn HR đã chuyển. */
    String SCOPE_FILTER = """
            where (:allDepartments = true or m.userId = :managerUserId or d.departmentId = :departmentId)
              and (:forwardedOnly = false or exists (
                    select 1 from ApplicationReview rv
                    where rv.application = a and rv.reviewerRole = 'HR' and rv.decision = 'Pass'))
            """;

    /** Bộ lọc của người dùng ({@code ApplicationSearch}); đặt sau {@link #SCOPE_FILTER}. */
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

    /**
     * Chỉ chuyển {@code Applied → AI_Screened}; đơn HR đã xử lý thì giữ nguyên.
     * Câu update viết tay không chạy {@code @PreUpdate} nên phải tự gán {@code updatedAt}.
     *
     * @return 1 nếu đã chuyển, 0 nếu đơn không còn ở {@code Applied}
     */
    @Modifying
    @Query("UPDATE Application a SET a.applicationStatus = 'AI_Screened', a.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE a.applicationId = :id AND a.applicationStatus = 'Applied'")
    int markScreened(@Param("id") Integer applicationId);

    /**
     * Đổi trạng thái khi duyệt, chỉ khi đơn còn ở một trong các trạng thái {@code from}
     * (người khác vừa xử lý thì không đè). Tự gán {@code updatedAt} vì câu update viết tay không chạy {@code @PreUpdate}.
     *
     * @return 1 nếu đã đổi, 0 nếu đơn không còn ở trạng thái mong đợi
     */
    @Modifying
    @Query("UPDATE Application a SET a.applicationStatus = :to, a.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE a.applicationId = :id AND a.applicationStatus IN :from")
    int transition(@Param("id") Integer applicationId, @Param("from") Collection<String> from,
                   @Param("to") String to);

    /**
     * Danh sách hồ sơ (5.1.22), mỗi đơn một dòng kèm điểm AI mới nhất.
     * {@code sort = 'score'}: điểm cao trước, chưa có điểm xếp cuối; {@code 'newest'}: chỉ theo ngày nộp.
     * Truyền {@code Pageable} không kèm sort: thứ tự cố định trong câu query.
     */
    @Query(value = """
            select new com.group2.rms.candidate.dto.ApplicationPipelineResponse(
                a.applicationId, u.fullName, u.email, jp.postingTitle, a.submissionDate, a.applicationStatus,
                ai.aiMatchScore)
            """ + PIPELINE_JOINS + """
                left join AIScreeningResult ai on ai.application = a and ai.aiScreeningId = (
                    select max(ai2.aiScreeningId) from AIScreeningResult ai2 where ai2.application = a)
            """ + SCOPE_FILTER + SEARCH_FILTER + """
            order by case when :sort = 'score' then ai.aiMatchScore end desc nulls last,
                     a.submissionDate desc, a.applicationId desc
            """,
            countQuery = "select count(a) " + PIPELINE_JOINS + SCOPE_FILTER + SEARCH_FILTER)
    Page<ApplicationPipelineResponse> searchPipeline(@Param("allDepartments") boolean allDepartments,
                                                     @Param("managerUserId") Integer managerUserId,
                                                     @Param("departmentId") Integer departmentId,
                                                     @Param("forwardedOnly") boolean forwardedOnly,
                                                     @Param("jobPostingId") Integer jobPostingId,
                                                     @Param("status") String status,
                                                     @Param("keyword") String keyword,
                                                     @Param("sort") String sort,
                                                     Pageable pageable);

    /** Các tin có đơn trong phạm vi người xem, cho ô lọc "Vị trí". */
    @Query("select distinct new com.group2.rms.candidate.dto.JobPostingOption(jp.jobPostingId, jp.postingTitle) "
            + PIPELINE_JOINS + SCOPE_FILTER + "order by jp.postingTitle")
    List<JobPostingOption> findPostingOptions(@Param("allDepartments") boolean allDepartments,
                                              @Param("managerUserId") Integer managerUserId,
                                              @Param("departmentId") Integer departmentId,
                                              @Param("forwardedOnly") boolean forwardedOnly);

    /** Đơn kèm mọi thứ trang chi tiết và kiểm quyền cần (ứng viên, tin, requisition, phòng ban, trưởng phòng). */
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
     * Lịch phỏng vấn của đơn, cho timeline. SQL thuần vì module interview đã phụ thuộc candidate:
     * import entity của interview vào đây sẽ thành vòng phụ thuộc.
     */
    @Query(value = "SELECT s.StartTime AS at, s.InterviewStatus AS status FROM InterviewSchedule s "
            + "WHERE s.ApplicationId = :id", nativeQuery = true)
    List<TimelineEventRow> findInterviewEvents(@Param("id") Integer applicationId);

    /** Offer của đơn, cho timeline. SQL thuần, cùng lý do với {@link #findInterviewEvents}. */
    @Query(value = "SELECT o.CreatedAt AS at, o.OfferStatus AS status FROM OfferProposal o "
            + "WHERE o.ApplicationId = :id", nativeQuery = true)
    List<TimelineEventRow> findOfferEvents(@Param("id") Integer applicationId);

    /** Một mốc đọc từ bảng của module khác: thời điểm + trạng thái gốc. */
    interface TimelineEventRow {
        LocalDateTime getAt();

        String getStatus();
    }
}
