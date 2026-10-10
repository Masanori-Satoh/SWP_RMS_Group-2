package com.group2.rms.candidate.repository;

import com.group2.rms.candidate.entity.ApplicationReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationReviewRepository extends JpaRepository<ApplicationReview, Integer> {

    boolean existsByApplication_ApplicationIdAndReviewerRoleAndDecision(Integer applicationId, String reviewerRole,
                                                                         String decision);

    @Query("select rv from ApplicationReview rv join fetch rv.reviewer where rv.application.applicationId = :id")
    List<ApplicationReview> findWithReviewerByApplicationId(@Param("id") Integer applicationId);
}
