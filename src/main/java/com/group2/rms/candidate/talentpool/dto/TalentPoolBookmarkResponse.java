package com.group2.rms.candidate.talentpool.dto;

import java.util.List;

public record TalentPoolBookmarkResponse(
        Integer candidateId,
        boolean saved,
        List<TalentPoolNoteResponse> notes) {
}
