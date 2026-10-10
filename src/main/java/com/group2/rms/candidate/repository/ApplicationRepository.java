package com.group2.rms.candidate.repository;

import com.group2.rms.candidate.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    @Query("SELECT a FROM Application a " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH c.account " +
           "LEFT JOIN FETCH a.jobPosting")
    List<Application> findAllWithCandidateAndJobPosting();

    boolean existsByCandidate_CandidateIdAndJobPosting_JobPostingId(Integer candidateId, Integer jobPostingId);

    /**
     * Chỉ chuyển {@code Applied → AI_Screened}; đơn HR đã xử lý thì giữ nguyên.
     * Câu update viết tay không chạy {@code @PreUpdate} nên phải tự gán {@code updatedAt}.
     *
     * @return 1 nếu đã chuyển, 0 nếu đơn không còn ở {@code Applied}
     */
    @Modifying
    @Query("UPDATE Application a SET a.applicationStatus = 'AI_Screened', a.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE a.applicationId = :id AND a.applicationStatus = 'Applied'")
    int markScreened(@Param("id") Integer applicationId);
}

