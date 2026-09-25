package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 3.7 JobRequisition - Yêu cầu tuyển dụng do Hiring Manager tạo.
 * ApprovalStatus: Draft, Pending_Director, Approved, Rejected
 * EmploymentType: Full-time, Part-time, Internship, Contract
 */
@Entity
@Table(name = "JobRequisition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRequisition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RequisitionId")
    private Integer requisitionId;

    @Column(name = "Title", nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DepartmentId", nullable = false, referencedColumnName = "DepartmentId")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HiringManagerId", nullable = false, referencedColumnName = "UserId")
    private User hiringManager;

    @Column(name = "NumberOfPositions", nullable = false)
    private Integer numberOfPositions;

    /**
     * Full-time, Part-time, Internship, Contract
     */
    @Column(name = "EmploymentType", nullable = false, length = 50)
    private String employmentType;

    @Column(name = "MinSalary", precision = 18, scale = 2)
    private BigDecimal minSalary;

    @Column(name = "MaxSalary", precision = 18, scale = 2)
    private BigDecimal maxSalary;

    @Column(name = "ReasonForHiring", length = 500)
    private String reasonForHiring;

    @Column(name = "JobDescription", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String jobDescription;

    @Column(name = "RequirementDetails", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String requirementDetails;

    /**
     * Draft, Pending_Director, Approved, Rejected
     */
    @Column(name = "ApprovalStatus", nullable = false, length = 30)
    private String approvalStatus;
}
