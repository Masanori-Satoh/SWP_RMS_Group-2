package com.group2.rms.career.dto;

import java.time.LocalDateTime;

public record CareerJobDetailResponse(
    Integer id, 
    String postingTitle, 
    String departmentName, 
    String workLocation, 
    String employmentType, 
    String salaryDisplay, 
    LocalDateTime applicationDeadline, 
    LocalDateTime postingDate, 
    String jobDescription, 
    String jobRequirements, 
    String benefits, 
    boolean hasApplied, 
    boolean isAcceptingApplications
) {}
