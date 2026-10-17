package com.group2.rms.requisition.dto;

import com.group2.rms.admin.dto.ActivityLogResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequisitionResponse {
    private Integer requisitionId;
    private String title;
    private String gender;
    private String workLocation;
    private String workModel;
    private Integer probationDays;
    private LocalDate expectedStartDate;
    private boolean editable;
    private boolean deletable;
    private boolean decidable;
    private boolean withdrawable;
    private boolean incomplete;
    private String rejectionReason;
    private String departmentName;
    private String hiringType;
    private String hiringManagerName;
    private Integer numberOfPositions;
    private String employmentType;
    private String approvalStatus;
    private LocalDateTime createdAt;
    private Integer recruitmentRound;
    private String requisitionCode;
    private String postingStatus;
    private Integer jobPostingId;

    // Các trường phục vụ màn Chi tiết
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String reasonForHiring;
    private String jobDescription;
    private String requirementDetails;

    private java.util.List<ScreeningCriteriaResponse> screeningCriteria;
    private java.util.List<ApprovalResponse> approvals;
    private java.util.List<ActivityLogResponse> activityLog;
}
