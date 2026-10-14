package com.group2.rms.candidate.dto;

// filter for talent pool
public record TalentPoolSearch(
        String keyword,
        Integer markedBy,
        Integer sourceJobId) {
}
