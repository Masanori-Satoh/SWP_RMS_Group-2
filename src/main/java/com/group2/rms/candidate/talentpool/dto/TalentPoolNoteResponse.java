package com.group2.rms.candidate.talentpool.dto;

import java.time.LocalDateTime;

public record TalentPoolNoteResponse(Integer id, String author, String content, LocalDateTime createdAt,
        String authorRole, boolean owned) {
    public TalentPoolNoteResponse(Integer id, String author, String content, LocalDateTime createdAt) {
        this(id, author, content, createdAt, null, false);
    }
}
