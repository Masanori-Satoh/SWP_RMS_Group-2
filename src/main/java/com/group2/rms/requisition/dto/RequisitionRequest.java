package com.group2.rms.requisition.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RequisitionRequest {
    private String action;
    private String requisitionCode;
    private String title;
    private Integer departmentId;
    private Integer recruitmentRound;
    private Integer numberOfPositions;
    private String employmentType;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String gender;
    private String workLocation;
    private String workModel;
    private String probationDuration;
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
    private LocalDate expectedStartDate;
    private String reasonForHiring;
    private String jobDescription;
    private String requirementDetails;
    @Builder.Default
    private List<ScreeningCriteriaRequest> screeningCriteria=new ArrayList<>();
}
