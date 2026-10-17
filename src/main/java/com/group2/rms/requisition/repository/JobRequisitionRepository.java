package com.group2.rms.requisition.repository;

import java.util.List;

import com.group2.rms.requisition.entity.JobRequisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository 
public interface JobRequisitionRepository extends JpaRepository<JobRequisition, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<JobRequisition> {
    
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from JobRequisition r where r.requisitionId = :id")
    java.util.Optional<JobRequisition> findForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);

    //Loc theo 1 trang thai
    List<JobRequisition> findByApprovalStatusOrderByCreatedAtDesc(String approvalStatus);

    //Lay tat ca req theo thu tu moi nhat, co phan trang
    Page<JobRequisition> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Có đợt tuyển nào được sao chép từ phiếu này không (phiếu này là OriginalRequisitionId của phiếu khác)
    boolean existsByOriginalRequisition_RequisitionId(Integer requisitionId);

    // Đợt lớn nhất trong chuỗi tuyển của phiếu gốc (gồm chính phiếu gốc và các đợt trỏ về nó)
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(MAX(r.recruitmentRound), 1) FROM JobRequisition r LEFT JOIN r.originalRequisition o WHERE r.requisitionId = :rootId OR o.requisitionId = :rootId")
    int findMaxRoundInChain(@org.springframework.data.repository.query.Param("rootId") Integer rootId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(r) > 0 FROM JobRequisition r WHERE LOWER(TRIM(r.title)) = LOWER(TRIM(:title)) AND r.department.departmentId = :departmentId AND r.approvalStatus = 'Draft' AND (:excludeId IS NULL OR r.requisitionId <> :excludeId)")
    boolean existsDraftByTitleAndDepartment(@org.springframework.data.repository.query.Param("title") String title, @org.springframework.data.repository.query.Param("departmentId") Integer departmentId, @org.springframework.data.repository.query.Param("excludeId") Integer excludeId);

    // Danh sách Hiring Manager đã tạo yêu cầu ở các trạng thái cho trước (dùng cho ô lọc "Người tạo yêu cầu")
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM JobRequisition r JOIN r.hiringManager u WHERE r.approvalStatus IN :statuses ORDER BY u.fullName ASC")
    List<com.group2.rms.user.entity.User> findCreatorsWithStatusIn(@org.springframework.data.repository.query.Param("statuses") java.util.Collection<String> statuses);
}
