package com.group2.rms.jobposting.repository;

import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Truy cập bảng JobPosting (và các câu đếm hồ sơ của tin).
 * Không chứa business rule: giá trị trạng thái truyền vào bằng tham số (JobPostingAccess.STATUS_*).
 * Các hàm findForUpdate, existsByRequisition_RequisitionId, findTopBy..., findLatestByRequisitionIds,
 * findPublishedJobs, findOpenPostings còn được module requisition / candidate / career gọi: đổi chữ ký phải báo nhóm.
 */
@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Integer>, JpaSpecificationExecutor<JobPosting> {

    // Khóa ghi khi sửa / chuyển trạng thái, tránh hai người thao tác cùng lúc
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from JobPosting j where j.jobPostingId = :id")
    Optional<JobPosting> findForUpdate(@Param("id") Integer id);

    /**
     * Màn danh sách: lấy luôn yêu cầu nguồn, phòng ban, người tạo trong cùng câu SELECT,
     * để mỗi dòng không phải chạy thêm câu lấy tên phòng ban / người tạo (chống N+1).
     */
    @Override
    @EntityGraph(attributePaths = {"requisition", "requisition.department", "createdBy"})
    Page<JobPosting> findAll(Specification<JobPosting> spec, Pageable pageable);

    boolean existsByRequisition_RequisitionId(Integer id);

    @Query("SELECT COALESCE(MAX(j.postingNo), 0) FROM JobPosting j WHERE j.requisition.requisitionId = :requisitionId")
    int findMaxPostingNo(@Param("requisitionId") Integer requisitionId);

    /** Số lần đăng lớn nhất của nhiều yêu cầu trong 1 câu. Mỗi dòng: [requisitionId, max PostingNo]. */
    @Query("""
            SELECT j.requisition.requisitionId, MAX(j.postingNo)
            FROM JobPosting j
            WHERE j.requisition.requisitionId IN :requisitionIds
            GROUP BY j.requisition.requisitionId
            """)
    List<Object[]> findMaxPostingNoByRequisitionIds(@Param("requisitionIds") Collection<Integer> requisitionIds);

    Optional<JobPosting> findTopByRequisition_RequisitionIdOrderByJobPostingIdDesc(Integer requisitionId);

    /**
     * Tin MỚI NHẤT (JobPostingId lớn nhất) của nhiều yêu cầu trong 1 câu (tránh N+1 ở màn danh sách yêu cầu).
     * Yêu cầu chưa có tin thì không có dòng nào.
     */
    @Query("SELECT p FROM JobPosting p WHERE p.requisition.requisitionId IN :requisitionIds "
            + "AND p.jobPostingId = (SELECT MAX(p2.jobPostingId) FROM JobPosting p2 WHERE p2.requisition = p.requisition)")
    List<JobPosting> findLatestByRequisitionIds(@Param("requisitionIds") Collection<Integer> requisitionIds);

    boolean existsByRequisition_RequisitionIdAndPostingStatusIn(Integer requisitionId, Collection<String> statuses);

    boolean existsByRequisition_RequisitionIdAndJobPostingIdNotAndPostingStatusIn(
            Integer requisitionId, Integer jobPostingId, Collection<String> statuses);

    /** Id các yêu cầu đang có tin ở các trạng thái cho trước (dùng tìm yêu cầu đã duyệt chưa có tin). */
    @Query("SELECT DISTINCT p.requisition.requisitionId FROM JobPosting p WHERE p.postingStatus IN :statuses")
    Set<Integer> findRequisitionIdsWithPostingStatusIn(@Param("statuses") Collection<String> statuses);

    /** Mọi lần đăng của một yêu cầu, theo thứ tự lần đăng. */
    List<JobPosting> findByRequisition_RequisitionIdOrderByPostingNoAsc(Integer requisitionId);

    // ----- Đếm hồ sơ (gộp cho cả trang, tránh N+1) -----

    /**
     * Đếm hồ sơ của nhiều tin trong 1 câu.
     * Mỗi dòng: [jobPostingId, tổng số hồ sơ, số hồ sơ có trạng thái newStatus (chưa ai xử lý)].
     */
    @Query("""
            SELECT a.jobPosting.jobPostingId, COUNT(a),
                   SUM(CASE WHEN a.applicationStatus = :newStatus THEN 1 ELSE 0 END)
            FROM Application a
            WHERE a.jobPosting.jobPostingId IN :ids
            GROUP BY a.jobPosting.jobPostingId
            """)
    List<Object[]> countApplicationsByPosting(@Param("ids") Collection<Integer> ids,
            @Param("newStatus") String newStatus);

    /** Số hồ sơ ở một trạng thái của nhiều tin (ví dụ đã nhận việc). Mỗi dòng: [jobPostingId, số hồ sơ]. */
    @Query("""
            SELECT a.jobPosting.jobPostingId, COUNT(a)
            FROM Application a
            WHERE a.jobPosting.jobPostingId IN :ids AND a.applicationStatus = :status
            GROUP BY a.jobPosting.jobPostingId
            """)
    List<Object[]> countApplicationsInStatusByPosting(@Param("ids") Collection<Integer> ids,
            @Param("status") String status);

    // ----- Số liệu cho trang chi tiết tin -----

    /**
     * Các câu đếm / liệt kê dưới đây chỉ tính đơn ứng tuyển chính thức: bỏ đề xuất của HM, gợi ý của HR
     * (xem ở khối Ứng viên tiềm năng) và đề xuất đã huỷ, cùng bộ lọc với danh sách /applications.
     */

    /** Số hồ sơ của một tin theo từng trạng thái. Mỗi dòng: [ApplicationStatus, số hồ sơ]. */
    @Query("SELECT a.applicationStatus, COUNT(a) FROM Application a "
            + "WHERE a.jobPosting.jobPostingId = :postingId AND " + ApplicationRepository.FORMAL_FILTER
            + " GROUP BY a.applicationStatus")
    List<Object[]> countApplicationsByStatus(@Param("postingId") Integer postingId);

    /** Số hồ sơ nộp từ một thời điểm trở đi (ví dụ trong 7 ngày gần nhất). */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.jobPosting.jobPostingId = :postingId AND a.submissionDate >= :since AND "
            + ApplicationRepository.FORMAL_FILTER)
    long countApplicationsSince(@Param("postingId") Integer postingId, @Param("since") LocalDateTime since);

    /** Số hồ sơ có điểm AI (lần chấm mới nhất) từ ngưỡng trở lên. */
    @Query("""
            SELECT COUNT(r) FROM AIScreeningResult r JOIN r.application a
            WHERE a.jobPosting.jobPostingId = :postingId
              AND r.aiMatchScore >= :threshold
              AND r.screenedAt = (SELECT MAX(r2.screenedAt) FROM AIScreeningResult r2 WHERE r2.application = r.application)
            """ + " AND " + ApplicationRepository.FORMAL_FILTER)
    long countApplicationsWithAiScoreAtLeast(@Param("postingId") Integer postingId,
            @Param("threshold") BigDecimal threshold);

    /** Hồ sơ mới nhất của tin. Mỗi dòng: [applicationId, tên ứng viên, ApplicationStatus, ngày nộp]. */
    @Query("SELECT a.applicationId, a.candidate.account.fullName, a.applicationStatus, a.submissionDate "
            + "FROM Application a WHERE a.jobPosting.jobPostingId = :postingId AND " + ApplicationRepository.FORMAL_FILTER
            + " ORDER BY a.submissionDate DESC")
    List<Object[]> findRecentApplications(@Param("postingId") Integer postingId, Pageable pageable);

    /** Điểm AI lần chấm mới nhất của các hồ sơ. Mỗi dòng: [applicationId, điểm]. */
    @Query("""
            SELECT r.application.applicationId, r.aiMatchScore FROM AIScreeningResult r
            WHERE r.application.applicationId IN :applicationIds
              AND r.screenedAt = (SELECT MAX(r2.screenedAt) FROM AIScreeningResult r2 WHERE r2.application = r.application)
            """)
    List<Object[]> findLatestAiScores(@Param("applicationIds") Collection<Integer> applicationIds);

    /** Giá trị một khóa cấu hình hệ thống (bảng SystemConfig), ví dụ AI_SCORE_THRESHOLD. */
    @Query("SELECT c.configValue FROM SystemConfig c WHERE c.configKey = :configKey")
    Optional<String> findConfigValue(@Param("configKey") String configKey);

    // ----- Bảng điều khiển HR (đếm bằng SQL, không tải mọi tin lên bộ nhớ) -----

    long countByPostingStatus(String postingStatus);

    /** Tin ở trạng thái cho trước, còn hạn nộp tại thời điểm now (hoặc không có hạn nộp). */
    @Query("""
            SELECT COUNT(p) FROM JobPosting p
            WHERE p.postingStatus = :status AND (p.applicationDeadline IS NULL OR p.applicationDeadline > :now)
            """)
    long countWithOpenDeadline(@Param("status") String status, @Param("now") LocalDateTime now);

    /** Tin ở trạng thái cho trước nhưng đã quá hạn nộp tại thời điểm now. */
    @Query("""
            SELECT COUNT(p) FROM JobPosting p
            WHERE p.postingStatus = :status AND p.applicationDeadline IS NOT NULL AND p.applicationDeadline <= :now
            """)
    long countWithPassedDeadline(@Param("status") String status, @Param("now") LocalDateTime now);

    /** Số tin theo phòng ban, nhiều nhất trước. Mỗi dòng: [tên phòng ban, số tin]. */
    @Query("""
            SELECT d.departmentName, COUNT(p)
            FROM JobPosting p JOIN p.requisition r JOIN r.department d
            GROUP BY d.departmentName
            ORDER BY COUNT(p) DESC
            """)
    List<Object[]> countByDepartment();

    /** Tin mới đăng (hoặc mới tạo nếu chưa đăng) gần nhất, kèm phòng ban trong cùng câu. */
    @EntityGraph(attributePaths = {"requisition", "requisition.department"})
    @Query("SELECT p FROM JobPosting p ORDER BY COALESCE(p.postingDate, p.createdAt) DESC, p.jobPostingId DESC")
    List<JobPosting> findRecent(Pageable pageable);

    // ----- Trang tuyển dụng công khai (module career) -----

    @Query("SELECT j FROM JobPosting j " +
           "WHERE j.postingStatus = 'Published' " +
           // Giữ tin cũ có PostingDate ở tương lai ngoài trang công khai cho tới khi dữ liệu được xử lý.
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
    List<JobPosting> findOpenPostings(@Param("now") LocalDateTime now);

}
