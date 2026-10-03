package com.group2.rms.career.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PublicJobDetailResponse(
    Integer id, 
    String postingTitle, 
    String departmentName, 
    String workLocation, 
    String employmentType, 
    String salaryDisplay, 
    LocalDateTime applicationDeadline, 
    LocalDateTime postingDate, 
    String jobDescription, 
    List<String> jobDescriptionList,
    String jobRequirements, 
    List<String> jobRequirementsList,
    String benefits, 
    List<String> benefitsList,
    boolean hasApplied, 
    boolean isAcceptingApplications
) {}
