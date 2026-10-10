package com.group2.rms.candidate.repository;

import com.group2.rms.candidate.entity.AIScreeningResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AIScreeningResultRepository extends JpaRepository<AIScreeningResult, Integer> {
}
