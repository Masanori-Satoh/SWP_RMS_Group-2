package com.group2.rms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.group2.rms.entity.JobRequisition;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository 
public interface JobRequisitionRepository extends JpaRepository<JobRequisition, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<JobRequisition> {
    
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from JobRequisition r where r.requisitionId = :id")
    java.util.Optional<JobRequisition> findForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
    //Hiring Manager xem req cua chinh minh
    List<JobRequisition> findByHiringManager_UserIdOrderByCreatedAtDesc(Integer userId);

    //Loc theo 1 trang thai
    List<JobRequisition> findByApprovalStatusOrderByCreatedAtDesc(String approvalStatus);

    //Loc theo nhieu trang thai cung luc
    List<JobRequisition> findByApprovalStatusInOrderByCreatedAtDesc(List<String> statuses);

    //Lay tat ca req theo thu tu moi nhat, co phan trang
    Page<JobRequisition> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
