package com.group2.rms.career.dto;

public record CandidateApplyProfileResponse(
    String fullName, 
    String email, 
    String phoneNumber,
    String linkedInUrl, 
    String portfolioUrl, 
    String address
) {}
