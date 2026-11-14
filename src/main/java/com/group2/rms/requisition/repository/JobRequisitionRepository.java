
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
    Optional<JobRequisition> findForUpdate(@Param("id") Integer id);

    List<JobRequisition> findByApprovalStatusOrderByCreatedAtDesc(
            String approvalStatus);

    @EntityGraph(attributePaths = "department")
    List<JobRequisition> findWithDepartmentByApprovalStatusOrderByCreatedAtDesc(String approvalStatus);

    boolean existsByOriginalRequisition_RequisitionId(Integer requisitionId);

    @Query("""
            SELECT a
            FROM RequisitionApproval a
            WHERE a.requisition.requisitionId = :requisitionId
            ORDER BY a.approvalDate DESC
            """)
    List<RequisitionApproval> findApprovalHistory(@Param("requisitionId") Integer requisitionId);

    @Query("""
            SELECT DISTINCT r.originalRequisition.requisitionId
            FROM JobRequisition r
            WHERE r.originalRequisition.requisitionId IN :ids
            """)
    List<Integer> findIdsWithLaterRounds(@Param("ids") Collection<Integer> ids);

    @Override
    @EntityGraph(attributePaths = { "department", "hiringManager" })
    Page<JobRequisition> findAll(Specification<JobRequisition> spec, Pageable pageable);

    /** Finds an existing Draft with the same normalized title and department. */
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
            @Param("excludeId") Integer excludeId);

    @Query("""
            SELECT DISTINCT u
            FROM JobRequisition r
            JOIN r.hiringManager u
            WHERE r.approvalStatus IN :statuses
            ORDER BY u.fullName ASC
            """)
    List<User> findCreatorsWithStatusIn(
            @Param("statuses") Collection<String> statuses);
}
