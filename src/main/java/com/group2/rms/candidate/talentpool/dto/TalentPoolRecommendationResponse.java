package com.group2.rms.candidate.talentpool.dto;

import java.time.LocalDateTime;

/** UI state derived from persisted draft, origin and structured review decisions. */
public record TalentPoolRecommendationResponse(Integer applicationId, Integer candidateId,
        Integer jobPostingId, String jobTitle, String fullName, String recommendedBy,
        LocalDateTime recommendedAt, String statusLabel, boolean pending, boolean canDecide) { }
