package com.group2.rms.jobposting.repository;

import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.jobposting.entity.JobPosting;
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

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/*
 * REPOSITORY: bảng JobPosting; vài hàm được module requisition / candidate / career gọi, đổi chữ ký phải báo nhóm.
 */
@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Integer>, JpaSpecificationExecutor<JobPosting> {

    // Khóa ghi khi sửa / chuyển trạng thái, tránh hai người thao tác cùng lúc
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from JobPosting j where j.jobPostingId = :id")
    Optional<JobPosting> findForUpdate(@Param("id") Integer id);

    /** Màn danh sách: lấy kèm yêu cầu, phòng ban, người tạo trong cùng câu (chống N+1). */
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

    /** Tin mới nhất của nhiều yêu cầu trong 1 câu (chống N+1); yêu cầu chưa có tin thì không có dòng. */
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

    /** Mỗi dòng: [jobPostingId, tổng số hồ sơ, số hồ sơ ở trạng thái newStatus]. */
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

    /** Hồ sơ mới nhất của tin. Mỗi dòng: [applicationId, tên ứng viên, ApplicationStatus, ngày nộp]. */
    @Query("SELECT a.applicationId, a.candidate.account.fullName, a.applicationStatus, a.submissionDate "
            + "FROM Application a WHERE a.jobPosting.jobPostingId = :postingId AND " + ApplicationRepository.FORMAL_FILTER
            + " ORDER BY a.submissionDate DESC")
    List<Object[]> findRecentApplications(@Param("postingId") Integer postingId, Pageable pageable);

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
