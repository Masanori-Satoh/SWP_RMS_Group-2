package com.group2.rms.candidate.talentpool.dto;

import java.time.LocalDateTime;

// List suggestion of HR for HM
public record TalentPoolSuggestionResponse(
        Integer applicationId,
        String fullName,
        String email,
        String phoneNumber,
        String suggestedBy,
        LocalDateTime suggestedAt) {
}
