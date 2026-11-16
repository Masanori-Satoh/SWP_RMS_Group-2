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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from JobPosting j where j.jobPostingId = :id")
    // Tìm tin và khóa để cập nhật.
    Optional<JobPosting> findForUpdate(@Param("id") Integer id);

    @Override
    @EntityGraph(attributePaths = {"requisition", "requisition.department", "createdBy"})
    // Tìm tin kèm dữ liệu yêu cầu nguồn.
    Page<JobPosting> findAll(Specification<JobPosting> spec, Pageable pageable);

    // Kiểm tra yêu cầu đã có tin.
    boolean existsByRequisition_RequisitionId(Integer id);

    @Query("SELECT COALESCE(MAX(j.postingNo), 0) FROM JobPosting j WHERE j.requisition.requisitionId = :requisitionId")
    // Lấy số lần đăng lớn nhất của yêu cầu.
    int findMaxPostingNo(@Param("requisitionId") Integer requisitionId);

    /** Số lần đăng lớn nhất của nhiều yêu cầu trong 1 câu. Mỗi dòng: [requisitionId, max PostingNo]. */
    @Query("""
            SELECT j.requisition.requisitionId, MAX(j.postingNo)
            FROM JobPosting j
            WHERE j.requisition.requisitionId IN :requisitionIds
            GROUP BY j.requisition.requisitionId
            """)
    // Lấy số lần đăng lớn nhất theo yêu cầu.
    List<Object[]> findMaxPostingNoByRequisitionIds(@Param("requisitionIds") Collection<Integer> requisitionIds);

    // Lấy tin mới nhất của yêu cầu.
    Optional<JobPosting> findTopByRequisition_RequisitionIdOrderByJobPostingIdDesc(Integer requisitionId);

    /** Tin mới nhất của nhiều yêu cầu trong 1 câu (chống N+1); yêu cầu chưa có tin thì không có dòng. */
    @Query("SELECT p FROM JobPosting p WHERE p.requisition.requisitionId IN :requisitionIds "
            + "AND p.jobPostingId = (SELECT MAX(p2.jobPostingId) FROM JobPosting p2 WHERE p2.requisition = p.requisition)")
    // Lấy tin mới nhất theo các yêu cầu.
    List<JobPosting> findLatestByRequisitionIds(@Param("requisitionIds") Collection<Integer> requisitionIds);

    // Kiểm tra yêu cầu có tin ở trạng thái được chọn.
    boolean existsByRequisition_RequisitionIdAndPostingStatusIn(Integer requisitionId, Collection<String> statuses);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.jobPosting.requisition.requisitionId = :requisitionId "
            + "AND a.applicationStatus = 'Hired'")
    // Đếm ứng viên đã tuyển của yêu cầu.
    long countHiredByRequisition(@Param("requisitionId") Integer requisitionId);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.jobPosting.requisition.requisitionId = :requisitionId "
            + "AND a.applicationStatus NOT IN ('Hired', 'Rejected', 'Withdrawn', 'Cancelled')")
    // Đếm hồ sơ còn xử lý của yêu cầu.
    long countActiveApplicationsByRequisition(@Param("requisitionId") Integer requisitionId);

    // Kiểm tra yêu cầu có tin khác đang mở.
    boolean existsByRequisition_RequisitionIdAndJobPostingIdNotAndPostingStatusIn(
            Integer requisitionId, Integer jobPostingId, Collection<String> statuses);

    @Query("SELECT DISTINCT p.requisition.requisitionId FROM JobPosting p WHERE p.postingStatus IN :statuses")
    // Lấy mã yêu cầu có tin ở trạng thái được chọn.
    Set<Integer> findRequisitionIdsWithPostingStatusIn(@Param("statuses") Collection<String> statuses);

    // Lấy các lần đăng theo thứ tự.
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
    // Đếm hồ sơ của các tin.
    List<Object[]> countApplicationsByPosting(@Param("ids") Collection<Integer> ids,
            @Param("newStatus") String newStatus);

    /** Số hồ sơ ở một trạng thái của nhiều tin (ví dụ đã nhận việc). Mỗi dòng: [jobPostingId, số hồ sơ]. */
    @Query("""
            SELECT a.jobPosting.jobPostingId, COUNT(a)
            FROM Application a
            WHERE a.jobPosting.jobPostingId IN :ids AND a.applicationStatus = :status
            GROUP BY a.jobPosting.jobPostingId
            """)
    // Đếm hồ sơ theo trạng thái của các tin.
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
    // Đếm hồ sơ theo trạng thái của tin.
    List<Object[]> countApplicationsByStatus(@Param("postingId") Integer postingId);

    /** Số hồ sơ nộp từ một thời điểm trở đi (ví dụ trong 7 ngày gần nhất). */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.jobPosting.jobPostingId = :postingId AND a.submissionDate >= :since AND "
            + ApplicationRepository.FORMAL_FILTER)
    // Đếm hồ sơ từ một thời điểm.
    long countApplicationsSince(@Param("postingId") Integer postingId, @Param("since") LocalDateTime since);

    /** Hồ sơ mới nhất của tin. Mỗi dòng: [applicationId, tên ứng viên, ApplicationStatus, ngày nộp]. */
    @Query("SELECT a.applicationId, a.candidate.account.fullName, a.applicationStatus, a.submissionDate "
            + "FROM Application a WHERE a.jobPosting.jobPostingId = :postingId AND " + ApplicationRepository.FORMAL_FILTER
            + " ORDER BY a.submissionDate DESC")
    // Lấy hồ sơ mới nhất của tin.
    List<Object[]> findRecentApplications(@Param("postingId") Integer postingId, Pageable pageable);

    // ----- Trang tuyển dụng công khai (module career) -----

    @Query("SELECT j FROM JobPosting j " +
           "WHERE j.postingStatus = 'Published' " +
           // Giữ tin cũ có PostingDate ở tương lai ngoài trang công khai cho tới khi dữ liệu được xử lý.
           "AND (j.postingDate IS NULL OR j.postingDate <= CURRENT_TIMESTAMP) " +
           "AND (j.applicationDeadline IS NULL OR j.applicationDeadline >= CURRENT_TIMESTAMP) " +
           "AND (:keyword IS NULL OR LOWER(j.postingTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:departmentId IS NULL OR j.requisition.department.departmentId = :departmentId) " +
           "AND (:employmentType IS NULL OR COALESCE(j.employmentType, j.requisition.employmentType) = :employmentType)")
    // Tìm tin đang đăng cho trang tuyển dụng.
    Page<JobPosting> findPublishedJobs(@Param("keyword") String keyword,
                                       @Param("departmentId") Integer departmentId,
                                       @Param("employmentType") String employmentType,
                                       Pageable pageable);

    @Query("SELECT j FROM JobPosting j WHERE j.postingStatus = 'Published' "
         + "AND (j.postingDate IS NULL OR j.postingDate <= :now) "
         + "AND (j.applicationDeadline IS NULL OR j.applicationDeadline >= :now) "
         + "ORDER BY j.postingDate DESC")
    // Tìm tin còn nhận hồ sơ.
    List<JobPosting> findOpenPostings(@Param("now") LocalDateTime now);

//     @Query("""
//         SELECT DISTINCT j.jobPostingId, j.postingTitle 
//         FROM JobPosting j 
//         JOIN Application a ON a.jobPosting = j
//         JOIN a.candidate c
//         WHERE c.isPotential = true 
//           AND a.applicationStatus <> 'HM_Suggested'
//     """)
//     List<Object[]> findJobsWithPotentialCandidates();
    
}
