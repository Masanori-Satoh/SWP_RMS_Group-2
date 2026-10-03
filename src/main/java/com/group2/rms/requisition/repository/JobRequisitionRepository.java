package com.group2.rms.requisition.repository;

import java.util.List;

import com.group2.rms.requisition.entity.JobRequisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository 
public interface JobRequisitionRepository extends JpaRepository<JobRequisition, Integer> {
    
    //Hiring Manager xem req cua chinh minh
    List<JobRequisition> findByHiringManager_UserIdOrderByCreatedAtDesc(Integer userId);

    //Loc theo 1 trang thai
    List<JobRequisition> findByApprovalStatusOrderByCreatedAtDesc(String approvalStatus);

    //Loc theo nhieu trang thai cung luc
    List<JobRequisition> findByApprovalStatusInOrderByCreatedAtDesc(List<String> statuses);

    //Lay tat ca req theo thu tu moi nhat, co phan trang
    Page<JobRequisition> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
