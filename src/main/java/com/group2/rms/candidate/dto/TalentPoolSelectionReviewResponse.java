package com.group2.rms.candidate.dto;

import java.util.List;
import java.time.LocalDateTime;

/** Read-only checks for a proposed selection; never creates a recruitment record. */
public record TalentPoolSelectionReviewResponse(List<Item> items, boolean submissionSupported) {
    public record Item(Integer candidateId, Integer sourceApplicationId, String candidateName,
                       String cvTitle, LocalDateTime submittedAt, String error) { }
}
