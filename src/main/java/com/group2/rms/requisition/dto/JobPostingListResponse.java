package com.group2.rms.requisition.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record JobPostingListResponse(
    Integer id, 
    String postingTitle, 
    String departmentName, 
    String workLocation, 
    String employmentType, 
    String salaryDisplay, 
    LocalDateTime applicationDeadline, 
    LocalDateTime postingDate
) {}
