package com.group2.rms.candidate.domain.repository;

import com.group2.rms.candidate.domain.entity.AIScreeningResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIScreeningResultRepository extends JpaRepository<AIScreeningResult, Integer> {

    /** Mọi lần chấm của một đơn, mới nhất trước. */
    List<AIScreeningResult> findByApplication_ApplicationIdOrderByAiScreeningIdDesc(Integer applicationId);
}
