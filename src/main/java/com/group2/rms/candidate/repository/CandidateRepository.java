package com.group2.rms.candidate.repository;

import com.group2.rms.candidate.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Integer> {
    Optional<Candidate> findByAccountUserId(Integer userId);

    java.util.List<Candidate> findAllByAccountUserIdIn(java.util.Collection<Integer> userIds);
}
