
package com.group2.rms.requisition.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.User;

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

/*
 * REPOSITORY: truy vấn yêu cầu tuyển dụng (danh sách theo Specification, khóa ghi khi duyệt, chuỗi đợt tuyển).
 */
@Repository
public interface JobRequisitionRepository
        extends JpaRepository<JobRequisition, Integer>,
        JpaSpecificationExecutor<JobRequisition> {

    // Tìm kiếm yêu cầu tuyển dụng theo ID và áp dụng khóa ghi để cập nhật an toàn
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM JobRequisition r WHERE r.requisitionId = :id")
    Optional<JobRequisition> findForUpdate(@Param("id") Integer id);

    // Lọc danh sách yêu cầu tuyển dụng theo trạng thái phê duyệt, sắp xếp giảm dần theo ngày tạo
    List<JobRequisition> findByApprovalStatusOrderByCreatedAtDesc(
            String approvalStatus
    );

    // Như trên nhưng lấy luôn phòng ban trong cùng câu SELECT (màn tin tuyển dụng hiện tên phòng ban từng dòng, chống N+1)
    @EntityGraph(attributePaths = "department")
    List<JobRequisition> findWithDepartmentByApprovalStatusOrderByCreatedAtDesc(String approvalStatus);

    // Kiểm tra xem có đợt tuyển dụng nào khác được sao chép/tạo từ yêu cầu tuyển dụng này hay không
    boolean existsByOriginalRequisition_RequisitionId(Integer requisitionId);

    // Trong các id cho trước, id nào đã có đợt tuyển sau sao chép từ nó (1 câu cho cả trang, tránh N+1)
    @Query("""
        SELECT DISTINCT r.originalRequisition.requisitionId
        FROM JobRequisition r
        WHERE r.originalRequisition.requisitionId IN :ids
        """)
    List<Integer> findIdsWithLaterRounds(@Param("ids") Collection<Integer> ids);

    // Màn danh sách: lấy kèm phòng ban + người tạo trong cùng câu (chống N+1)
    @Override
    @EntityGraph(attributePaths = {"department", "hiringManager"})
    Page<JobRequisition> findAll(Specification<JobRequisition> spec, Pageable pageable);

    // Tìm số vòng/đợt tuyển dụng lớn nhất trong chuỗi liên kết các yêu cầu tuyển dụng
    @Query("""
        SELECT COALESCE(MAX(r.recruitmentRound), 1)
        FROM JobRequisition r
        LEFT JOIN r.originalRequisition o
        WHERE r.requisitionId = :rootId
           OR o.requisitionId = :rootId
        """)
    int findMaxRoundInChain(@Param("rootId") Integer rootId);

    // Kiểm tra đã tồn tại bản nháp trùng tiêu đề và phòng ban hay chưa
    @Query("""
        SELECT COUNT(r) > 0
        FROM JobRequisition r
        WHERE LOWER(TRIM(r.title)) = LOWER(TRIM(:title))
          AND r.department.departmentId = :departmentId
          AND r.approvalStatus = 'Draft'
          AND (:excludeId IS NULL OR r.requisitionId <> :excludeId)
        """)
    boolean existsDraftByTitleAndDepartment(
            @Param("title") String title,
            @Param("departmentId") Integer departmentId,
            @Param("excludeId") Integer excludeId
    );

    // Lấy danh sách người tạo duy nhất thuộc các trạng thái chỉ định, sắp xếp theo tên
    @Query("""
        SELECT DISTINCT u
        FROM JobRequisition r
        JOIN r.hiringManager u
        WHERE r.approvalStatus IN :statuses
        ORDER BY u.fullName ASC
        """)
    List<User> findCreatorsWithStatusIn(
            @Param("statuses") Collection<String> statuses
    );
}
