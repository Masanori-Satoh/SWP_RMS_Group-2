package com.group2.rms.candidate.domain.repository;

import com.group2.rms.candidate.domain.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
//new interface cho dynacmic filter nha các bảo bối
public interface CandidateRepository extends JpaRepository<Candidate, Integer>, JpaSpecificationExecutor<Candidate> {
    Optional<Candidate> findByAccountUserId(Integer userId);

    Optional<Candidate> findByAccount_UsernameIgnoreCase(String username);

    List<Candidate> findAllByAccountUserIdIn(Collection<Integer> userIds);
}
