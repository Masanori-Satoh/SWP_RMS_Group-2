package com.group2.rms.requisition.repository;

import com.group2.rms.requisition.entity.JobPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Integer> {

    @Query("SELECT j FROM JobPosting j " +
           "WHERE j.postingStatus = 'Published' " +
           "AND j.applicationDeadline >= CURRENT_TIMESTAMP " +
           "AND (:keyword IS NULL OR LOWER(j.postingTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:departmentId IS NULL OR j.requisition.department.departmentId = :departmentId) " +
           "AND (:employmentType IS NULL OR j.requisition.employmentType = :employmentType)")
    Page<JobPosting> findPublishedJobs(@Param("keyword") String keyword, 
                                       @Param("departmentId") Integer departmentId, 
                                       @Param("employmentType") String employmentType, 
                                       Pageable pageable);

    @Query("SELECT j FROM JobPosting j WHERE j.postingStatus = 'Published' "
         + "AND (j.applicationDeadline IS NULL OR j.applicationDeadline >= :now) "
         + "ORDER BY j.postingDate DESC")
    java.util.List<JobPosting> findOpenPostings(@Param("now") java.time.LocalDateTime now);

    @Query("SELECT j FROM JobPosting j WHERE j.jobPostingId = :id "
         + "AND j.postingStatus = 'Published' "
         + "AND (j.applicationDeadline IS NULL OR j.applicationDeadline >= :now)")
    java.util.Optional<JobPosting> findOpenPosting(@Param("id") int id, @Param("now") java.time.LocalDateTime now);
}
