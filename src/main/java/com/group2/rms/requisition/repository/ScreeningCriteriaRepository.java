package com.group2.rms.requisition.repository;

import com.group2.rms.requisition.entity.ScreeningCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreeningCriteriaRepository extends JpaRepository<ScreeningCriteria, Integer> {
    List<ScreeningCriteria> findByRequisition_RequisitionId(Integer requisitionId);
}
