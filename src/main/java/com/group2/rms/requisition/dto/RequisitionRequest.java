package com.group2.rms.requisition.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequisitionRequest {

    private String action;

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Department ID is required")
    private Integer departmentId;

    @Min(value = 1, message = "Number of positions must be at least 1")
    private Integer numberOfPositions;

    @NotBlank(message = "Employment type is required")
    private String employmentType;

    @Min(value = 0, message = "Min Salary must be at least 0")
    private BigDecimal minSalary;

    @Min(value = 0, message = "Max Salary must be at least 0")
    private BigDecimal maxSalary;

    @NotBlank(message = "Reason for Hiring is required")
    @jakarta.validation.constraints.Size(max = 500, message = "Reason for Hiring cannot exceed 500 characters")
    private String reasonForHiring;

    @NotBlank(message = "Job description is required")
    @jakarta.validation.constraints.Size(max = 2000, message = "Job description cannot exceed 2000 characters")
    private String jobDescription;

    @NotBlank(message = "Candidate requirements are required")
    @jakarta.validation.constraints.Size(max = 2000, message = "Requirements cannot exceed 2000 characters")
    private String requirementDetails;

    @NotNull(message = "Hiring manager is required")
    private Integer hiringManagerId;

    private java.util.List<ScreeningCriteriaRequest> screeningCriteria;
}
