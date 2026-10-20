package com.group2.rms.requisition.entity;

import com.group2.rms.core.base.BaseEntity;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 3.6 JobRequisition - Yêu cầu tuyển dụng do Hiring Manager tạo.
 * ApprovalStatus: Draft, Pending_Director, Approved, Rejected (luồng hiện tại không dùng Closed / Cancelled)
 * Đợt tuyển: đợt 1 có originalRequisition = null; đợt > 1 trỏ về phiếu đợt 1 (CK_JobRequisition_RoundOrigin).
 * EmploymentType: Full-time, Part-time, Internship, Contract
 * Mã yêu cầu (RequisitionCode) do service sinh sau lần lưu đầu: REQ-0025 = "REQ-" + RequisitionId đệm 4 chữ số.
 * nullable = false khớp các cột NOT NULL trong database/schema/db.sql (chỉ là mô tả, DB mới là nơi ràng buộc thật).
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
