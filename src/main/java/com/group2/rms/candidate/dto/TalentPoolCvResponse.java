package com.group2.rms.candidate.dto;

import java.time.LocalDateTime;

// List CV
public record TalentPoolCvResponse(
        Integer applicationId,
        String jobTitle,
        LocalDateTime submittedAt) {
}
