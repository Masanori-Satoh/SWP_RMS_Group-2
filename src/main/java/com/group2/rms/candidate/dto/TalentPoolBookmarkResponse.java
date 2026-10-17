package com.group2.rms.candidate.dto;

import java.util.List;

public record TalentPoolBookmarkResponse(Integer candidateId, boolean saved, List<TalentPoolNoteResponse> notes) {
}
