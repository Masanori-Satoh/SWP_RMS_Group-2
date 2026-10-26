package com.group2.rms.candidate.talentpool.dto;

import java.time.LocalDateTime;

// List CV
public record TalentPoolCvResponse(
        Integer applicationId,
        String jobTitle,
        LocalDateTime submittedAt) {
}
