package com.group2.rms.candidate.talentpool.dto;

import java.time.LocalDateTime;

// Note of potential candidate
public record TalentPoolNoteResponse(
        Integer id,
        String author,
        String content,
        LocalDateTime createdAt) {
}
