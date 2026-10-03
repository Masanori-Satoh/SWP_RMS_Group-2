package com.group2.rms.requisition.repository;

import java.util.List;

import com.group2.rms.requisition.entity.RequisitionApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequisitionApprovalRepository extends JpaRepository<RequisitionApproval, Integer> {
    List<RequisitionApproval> findByRequisition_RequisitionIdOrderByApprovalDateDesc(Integer requisitionId);
}
