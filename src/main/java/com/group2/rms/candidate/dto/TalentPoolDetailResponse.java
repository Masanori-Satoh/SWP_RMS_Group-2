package com.group2.rms.candidate.dto;

import java.util.List;
// detail of a candidate
public record TalentPoolDetailResponse(
        TalentPoolCandidateResponse candidate,
        boolean potential,
        List<TalentPoolNoteResponse> notes,
        List<TalentPoolCvResponse> cvs) {
}
