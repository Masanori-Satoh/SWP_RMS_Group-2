package com.group2.rms.dto.response;

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
public class RequisitionResponseDto {
    private Integer requisitionId;
    private String title;
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

    private java.util.List<ScreeningCriteriaDto> screeningCriteria;
    private java.util.List<ApprovalResponseDto> approvals;
    private java.util.List<ActivityLogDto> activityLog;
}
