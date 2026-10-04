package com.group2.rms.candidate.repository;

import com.group2.rms.candidate.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    @Query("SELECT a FROM Application a " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH c.account " +
           "LEFT JOIN FETCH a.jobPosting")
    List<Application> findAllWithCandidateAndJobPosting();
}

