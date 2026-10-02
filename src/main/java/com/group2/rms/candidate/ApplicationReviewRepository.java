package com.group2.rms.candidate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationReviewRepository extends JpaRepository<ApplicationReview, Integer> {
}
