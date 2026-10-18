package com.group2.rms.jobposting.repository;

import com.group2.rms.jobposting.entity.JobPosting;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Integer>, JpaSpecificationExecutor<JobPosting> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from JobPosting j where j.jobPostingId = :id")
    java.util.Optional<JobPosting> findForUpdate(@Param("id") Integer id);

    boolean existsByRequisition_RequisitionId(Integer id);

    @Query("SELECT COALESCE(MAX(j.postingNo), 0) FROM JobPosting j WHERE j.requisition.requisitionId = :requisitionId")
    int findMaxPostingNo(@Param("requisitionId") Integer requisitionId);

    java.util.Optional<JobPosting> findTopByRequisition_RequisitionIdOrderByJobPostingIdDesc(Integer requisitionId);

    /**
     * Tin MỚI NHẤT (JobPostingId lớn nhất) của nhiều yêu cầu trong 1 câu (tránh N+1 ở màn danh sách yêu cầu).
     * Yêu cầu chưa có tin thì không có dòng nào.
     */
    @Query("SELECT p FROM JobPosting p WHERE p.requisition.requisitionId IN :requisitionIds "
            + "AND p.jobPostingId = (SELECT MAX(p2.jobPostingId) FROM JobPosting p2 WHERE p2.requisition = p.requisition)")
    java.util.List<JobPosting> findLatestByRequisitionIds(
            @Param("requisitionIds") java.util.Collection<Integer> requisitionIds);

    boolean existsByRequisition_RequisitionIdAndPostingStatusIn(Integer requisitionId, java.util.Collection<String> statuses);

    /**
     * Đếm hồ sơ của nhiều tin trong 1 câu (tránh N+1 ở màn danh sách).
     * Mỗi dòng: [jobPostingId, tổng số hồ sơ, số hồ sơ "Applied" (chưa ai xử lý)].
     */
    @Query("SELECT a.jobPosting.jobPostingId, COUNT(a), "
         + "SUM(CASE WHEN a.applicationStatus = 'Applied' THEN 1 ELSE 0 END) "
         + "FROM Application a WHERE a.jobPosting.jobPostingId IN :ids GROUP BY a.jobPosting.jobPostingId")
    java.util.List<Object[]> countApplicationsByPosting(@Param("ids") java.util.Collection<Integer> ids);

    // ----- Số liệu cho trang chi tiết tin -----

    /** Số hồ sơ của một tin theo từng trạng thái. Mỗi dòng: [ApplicationStatus, số hồ sơ]. */
    @Query("SELECT a.applicationStatus, COUNT(a) FROM Application a "
         + "WHERE a.jobPosting.jobPostingId = :postingId GROUP BY a.applicationStatus")
    java.util.List<Object[]> countApplicationsByStatus(@Param("postingId") Integer postingId);

    /** Số hồ sơ nộp từ một thời điểm trở đi (ví dụ trong 7 ngày gần nhất). */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.jobPosting.jobPostingId = :postingId AND a.submissionDate >= :since")
    long countApplicationsSince(@Param("postingId") Integer postingId, @Param("since") java.time.LocalDateTime since);

    /** Số hồ sơ có điểm AI (lần chấm mới nhất) từ ngưỡng trở lên. */
    @Query("SELECT COUNT(r) FROM AIScreeningResult r WHERE r.application.jobPosting.jobPostingId = :postingId "
         + "AND r.aiMatchScore >= :threshold "
         + "AND r.screenedAt = (SELECT MAX(r2.screenedAt) FROM AIScreeningResult r2 WHERE r2.application = r.application)")
    long countApplicationsWithAiScoreAtLeast(@Param("postingId") Integer postingId,
                                             @Param("threshold") java.math.BigDecimal threshold);

    /** Hồ sơ mới nhất của tin. Mỗi dòng: [applicationId, tên ứng viên, ApplicationStatus, ngày nộp]. */
    @Query("SELECT a.applicationId, a.candidate.account.fullName, a.applicationStatus, a.submissionDate "
         + "FROM Application a WHERE a.jobPosting.jobPostingId = :postingId ORDER BY a.submissionDate DESC")
    java.util.List<Object[]> findRecentApplications(@Param("postingId") Integer postingId, Pageable pageable);

    /** Điểm AI lần chấm mới nhất của các hồ sơ. Mỗi dòng: [applicationId, điểm]. */
    @Query("SELECT r.application.applicationId, r.aiMatchScore FROM AIScreeningResult r "
         + "WHERE r.application.applicationId IN :applicationIds "
         + "AND r.screenedAt = (SELECT MAX(r2.screenedAt) FROM AIScreeningResult r2 WHERE r2.application = r.application)")
    java.util.List<Object[]> findLatestAiScores(@Param("applicationIds") java.util.Collection<Integer> applicationIds);

    /** Số hồ sơ đã nhận việc của nhiều tin. Mỗi dòng: [jobPostingId, số hồ sơ Hired]. */
    @Query("SELECT a.jobPosting.jobPostingId, COUNT(a) FROM Application a "
         + "WHERE a.jobPosting.jobPostingId IN :ids AND a.applicationStatus = 'Hired' GROUP BY a.jobPosting.jobPostingId")
    java.util.List<Object[]> countHiredByPosting(@Param("ids") java.util.Collection<Integer> ids);

    /** Mọi lần đăng của một yêu cầu, theo thứ tự lần đăng. */
    java.util.List<JobPosting> findByRequisition_RequisitionIdOrderByPostingNoAsc(Integer requisitionId);

    /** Ngưỡng điểm AI "đạt" trong cấu hình hệ thống (SystemConfig.AI_SCORE_THRESHOLD). */
    @Query("SELECT c.configValue FROM SystemConfig c WHERE c.configKey = 'AI_SCORE_THRESHOLD'")
    java.util.Optional<String> findAiScoreThreshold();

    /** Những người đã tạo tin, cho ô lọc "Người tạo" (Admin). */
    @Query("SELECT DISTINCT u FROM JobPosting p JOIN p.createdBy u ORDER BY u.fullName ASC")
    java.util.List<com.group2.rms.user.entity.User> findPostingCreators();

    /** Id các yêu cầu đang có tin ở các trạng thái cho trước (dùng tìm yêu cầu đã duyệt chưa có tin). */
    @Query("SELECT DISTINCT p.requisition.requisitionId FROM JobPosting p WHERE p.postingStatus IN :statuses")
    java.util.Set<Integer> findRequisitionIdsWithPostingStatusIn(@Param("statuses") java.util.Collection<String> statuses);

    boolean existsByRequisition_RequisitionIdAndJobPostingIdNotAndPostingStatusIn(
            Integer requisitionId, Integer jobPostingId, java.util.Collection<String> statuses);

    @Query("SELECT j FROM JobPosting j " +
           "WHERE j.postingStatus = 'Published' " +
           // Tin hẹn đăng (PostingDate ở tương lai) chưa hiện cho ứng viên
           "AND (j.postingDate IS NULL OR j.postingDate <= CURRENT_TIMESTAMP) " +
           "AND (j.applicationDeadline IS NULL OR j.applicationDeadline >= CURRENT_TIMESTAMP) " +
           "AND (:keyword IS NULL OR LOWER(j.postingTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:departmentId IS NULL OR j.requisition.department.departmentId = :departmentId) " +
           "AND (:employmentType IS NULL OR j.requisition.employmentType = :employmentType)")
    Page<JobPosting> findPublishedJobs(@Param("keyword") String keyword,
                                       @Param("departmentId") Integer departmentId,
                                       @Param("employmentType") String employmentType,
                                       Pageable pageable);

    @Query("SELECT j FROM JobPosting j WHERE j.postingStatus = 'Published' "
         + "AND (j.postingDate IS NULL OR j.postingDate <= :now) "
         + "AND (j.applicationDeadline IS NULL OR j.applicationDeadline >= :now) "
         + "ORDER BY j.postingDate DESC")
    java.util.List<JobPosting> findOpenPostings(@Param("now") java.time.LocalDateTime now);
}
