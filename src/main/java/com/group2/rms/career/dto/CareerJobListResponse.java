package com.group2.rms.career.dto;

import java.time.LocalDateTime;

public record CareerJobListResponse(
    Integer id, 
    String postingTitle, 
    String departmentName, 
    String workLocation, 
    String employmentType, 
    String salaryDisplay, 
    LocalDateTime applicationDeadline, 
    LocalDateTime postingDate
) {}
