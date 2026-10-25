package com.group2.rms.candidate.talentpool.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TalentPoolRecommendationDecisionRequest(
        @NotNull @Pattern(regexp = "Pass|Fail") String decision) { }
