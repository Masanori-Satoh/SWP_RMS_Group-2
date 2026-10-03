package com.group2.rms.requisition.dto;

import com.group2.rms.admin.ActivityLogResponse;

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
    private Long version;
    private String gender;
    private String workLocation;
    private String workingHours;
    private java.time.LocalDate expectedStartDate;
    private boolean editable;
    private boolean deletable;
    private boolean decidable;
    private boolean withdrawable;
    private boolean incomplete;
    private String rejectionReason;
    private java.util.List<RequisitionTimelineResponse> timeline;
    private String departmentName;
    private String hiringManagerName;
    private Integer numberOfPositions;
    private String employmentType;
    private String approvalStatus;
    private LocalDateTime createdAt;

    // Các trường phục vụ màn Chi tiết
    private java.math.BigDecimal minSalary;
    private java.math.BigDecimal maxSalary;
    private String reasonForHiring;
    private String jobDescription;
    private String requirementDetails;

    private java.util.List<ScreeningCriteriaResponse> screeningCriteria;
    private java.util.List<ApprovalResponse> approvals;
    private java.util.List<ActivityLogResponse> activityLog;
}
