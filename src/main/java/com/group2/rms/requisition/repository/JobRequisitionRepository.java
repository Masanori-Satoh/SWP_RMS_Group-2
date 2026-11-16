
package com.group2.rms.requisition.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
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

@Repository
public interface JobRequisitionRepository
        extends JpaRepository<JobRequisition, Integer>,
        JpaSpecificationExecutor<JobRequisition> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM JobRequisition r WHERE r.requisitionId = :id")
    // Tìm yêu cầu và khóa để cập nhật.
    Optional<JobRequisition> findForUpdate(@Param("id") Integer id);

    // Lấy yêu cầu theo trạng thái, mới trước.
    List<JobRequisition> findByApprovalStatusOrderByCreatedAtDesc(
            String approvalStatus);

    @EntityGraph(attributePaths = "department")
    // Lấy yêu cầu cùng phòng ban theo trạng thái.
    List<JobRequisition> findWithDepartmentByApprovalStatusOrderByCreatedAtDesc(String approvalStatus);

    // Kiểm tra yêu cầu đã có đợt tuyển sau.
    boolean existsByOriginalRequisition_RequisitionId(Integer requisitionId);

    @Query("""
            SELECT a
            FROM RequisitionApproval a
            WHERE a.requisition.requisitionId = :requisitionId
            ORDER BY a.approvalDate DESC
            """)
    // Lấy lịch sử phê duyệt mới trước.
    List<RequisitionApproval> findApprovalHistory(@Param("requisitionId") Integer requisitionId);

    @Query("""
            SELECT DISTINCT r.originalRequisition.requisitionId
            FROM JobRequisition r
            WHERE r.originalRequisition.requisitionId IN :ids
            """)
    // Lấy mã yêu cầu có đợt tuyển sau.
    List<Integer> findIdsWithLaterRounds(@Param("ids") Collection<Integer> ids);

    @Override
    @EntityGraph(attributePaths = { "department", "hiringManager" })
    // Tìm yêu cầu kèm phòng ban và người tạo.
    Page<JobRequisition> findAll(Specification<JobRequisition> spec, Pageable pageable);

    @Query("""
            SELECT DISTINCT u
            FROM JobRequisition r
            JOIN r.hiringManager u
            WHERE r.approvalStatus IN :statuses
            ORDER BY u.fullName ASC
            """)
    // Lấy người tạo theo các trạng thái.
    List<User> findCreatorsWithStatusIn(
            @Param("statuses") Collection<String> statuses);
}
