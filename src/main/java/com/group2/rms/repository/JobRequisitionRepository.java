package com.group2.rms.repository;

import com.group2.rms.entity.JobRequisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRequisitionRepository extends JpaRepository<JobRequisition, Integer> {
}
