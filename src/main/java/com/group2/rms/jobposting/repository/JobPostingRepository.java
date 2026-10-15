package com.group2.rms.jobposting.repository;

import com.group2.rms.jobposting.entity.JobPosting;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Integer>, JpaSpecificationExecutor<JobPosting> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from JobPosting j where j.jobPostingId = :id")
    java.util.Optional<JobPosting> findForUpdate(@Param("id") Integer id);

    boolean existsByRequisition_RequisitionId(Integer id);

    java.util.Optional<JobPosting> findTopByRequisition_RequisitionIdOrderByJobPostingIdDesc(Integer requisitionId);

    boolean existsByRequisition_RequisitionIdAndPostingStatusIn(Integer requisitionId, java.util.Collection<String> statuses);

    boolean existsByRequisition_RequisitionIdAndJobPostingIdNotAndPostingStatusIn(
            Integer requisitionId, Integer jobPostingId, java.util.Collection<String> statuses);

    java.util.List<JobPosting> findByRequisition_RequisitionIdIn(java.util.Collection<Integer> requisitionIds);

    @Query("SELECT j FROM JobPosting j " +
           "WHERE j.postingStatus = 'Published' " +
           "AND (j.applicationDeadline IS NULL OR j.applicationDeadline >= CURRENT_TIMESTAMP) " +
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
