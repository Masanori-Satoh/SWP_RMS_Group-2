package com.group2.rms.repository;

import com.group2.rms.interview.InterviewFinalResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewFinalResultRepository extends JpaRepository<InterviewFinalResult, Integer> {

    List<InterviewFinalResult> findByFinalDecisionIgnoreCase(String finalDecision);

    Optional<InterviewFinalResult> findByInterviewSchedule_InterviewId(Integer interviewId);

    @Query("SELECT r FROM InterviewFinalResult r " +
           "JOIN r.interviewSchedule s " +
           "WHERE s.application.applicationId = :applicationId " +
           "ORDER BY r.approvedAt DESC")
    List<InterviewFinalResult> findByApplicationIdOrderByApprovedAtDesc(@Param("applicationId") Integer applicationId);

    @Query("SELECT r FROM InterviewFinalResult r " +
           "JOIN FETCH r.interviewSchedule s " +
           "JOIN FETCH s.application a " +
           "JOIN FETCH a.candidate c " +
           "JOIN FETCH c.account u " +
           "LEFT JOIN FETCH a.jobPosting jp " +
           "LEFT JOIN FETCH jp.requisition req " +
           "LEFT JOIN FETCH req.department d " +
           "LEFT JOIN FETCH r.hiringManager hm " +
           "WHERE LOWER(r.finalDecision) = 'passed'")
    List<InterviewFinalResult> findAllPassedWithDetails();
}
