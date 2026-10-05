package com.group2.rms.requisition.repository;

import java.util.List;

import com.group2.rms.requisition.entity.JobRequisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository 
public interface JobRequisitionRepository extends JpaRepository<JobRequisition, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<JobRequisition> {
    
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from JobRequisition r where r.requisitionId = :id")
    java.util.Optional<JobRequisition> findForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
    //Hiring Manager xem req cua chinh minh
    List<JobRequisition> findByHiringManager_UserIdOrderByCreatedAtDesc(Integer userId);

    //Loc theo 1 trang thai
    List<JobRequisition> findByApprovalStatusOrderByCreatedAtDesc(String approvalStatus);

    //Loc theo nhieu trang thai cung luc
    List<JobRequisition> findByApprovalStatusInOrderByCreatedAtDesc(List<String> statuses);

    //Lay tat ca req theo thu tu moi nhat, co phan trang
    Page<JobRequisition> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT r.title FROM JobRequisition r WHERE r.title IS NOT NULL AND TRIM(r.title) <> '' ORDER BY r.title ASC")
    List<String> findDistinctTitles();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT r.title FROM JobRequisition r WHERE r.department.departmentId IN (:departmentIds) AND r.title IS NOT NULL AND r.title <> '' ORDER BY r.title ASC")
    List<String> findDistinctTitlesByDepartmentIds(@org.springframework.data.repository.query.Param("departmentIds") List<Integer> departmentIds);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(MAX(r.recruitmentRound), 0) FROM JobRequisition r WHERE LOWER(TRIM(r.title)) = LOWER(TRIM(:title)) AND r.department.departmentId = :departmentId")
    Integer findMaxRecruitmentRound(@org.springframework.data.repository.query.Param("title") String title, @org.springframework.data.repository.query.Param("departmentId") Integer departmentId);
}
