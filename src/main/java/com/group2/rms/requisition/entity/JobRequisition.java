package com.group2.rms.requisition.entity;

import com.group2.rms.core.base.BaseEntity;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/*
 * ENTITY: yêu cầu tuyển dụng; đợt 1 không có phiếu gốc, đợt sau trỏ về phiếu đợt 1.
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

    @Column(name = "RequisitionCode", length = 50)
    private String requisitionCode;

    @Column(name = "Title", nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DepartmentId", nullable = false, referencedColumnName = "DepartmentId")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HiringManagerId", nullable = false, referencedColumnName = "UserId")
    private User hiringManager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", referencedColumnName = "UserId")
    private User createdBy;

    @Column(name = "RecruitmentRound", nullable = false)
    private Integer recruitmentRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OriginalRequisitionId", referencedColumnName = "RequisitionId")
    private JobRequisition originalRequisition;

    /**
     * New, Replacement (NULL = chưa xác định)
     */
    @Column(name = "HiringType", length = 20)
    private String hiringType;

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

    @Column(name = "ReasonForHiring", length = 2000, columnDefinition = "NVARCHAR(2000)")
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

    @Column(name = "RequiredGender", nullable = false, length = 20)
    private String gender;
    @Column(name = "WorkLocation", length = 255)
    private String workLocation;
    @Column(name = "WorkModel", nullable = false, length = 20)
    private String workModel;
    @Column(name = "ProbationDays")
    private Integer probationDays;
    @Column(name = "SubmittedAt")
    private java.time.LocalDateTime submittedAt;
    // Cột có trong DB nhưng luồng requisition hiện chưa ghi (không có trạng thái đóng yêu cầu)
    @Column(name = "ClosedAt")
    private java.time.LocalDateTime closedAt;

    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<ScreeningCriteria> screeningCriteria;
}
