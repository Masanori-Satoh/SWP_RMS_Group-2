package com.group2.rms.requisition.repository;

import com.group2.rms.requisition.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Integer> {
}
