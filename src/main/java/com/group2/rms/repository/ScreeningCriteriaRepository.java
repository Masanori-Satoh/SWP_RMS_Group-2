package com.group2.rms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.group2.rms.entity.ScreeningCriteria;

@Repository
public interface ScreeningCriteriaRepository extends JpaRepository<ScreeningCriteria, Integer> {
    java.util.List<ScreeningCriteria> findByRequisition_RequisitionId(Integer requisitionId);
    void deleteByRequisition_RequisitionId(Integer requisitionId);
}
