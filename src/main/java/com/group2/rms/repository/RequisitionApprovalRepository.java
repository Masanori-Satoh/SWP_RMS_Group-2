package com.group2.rms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.group2.rms.entity.RequisitionApproval;

@Repository
public interface RequisitionApprovalRepository extends JpaRepository<RequisitionApproval, Integer> {
    List<RequisitionApproval> findByRequisition_RequisitionIdOrderByApprovalDateDesc(Integer requisitionId);
}
