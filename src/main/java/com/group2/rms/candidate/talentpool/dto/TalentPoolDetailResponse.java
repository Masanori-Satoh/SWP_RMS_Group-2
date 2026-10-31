package com.group2.rms.candidate.talentpool.dto;

import java.time.LocalDate;
import java.util.List;
// detail of a candidate
public record TalentPoolDetailResponse(
        TalentPoolCandidateResponse candidate,
        boolean potential,
        List<TalentPoolNoteResponse> notes,
        List<TalentPoolCvResponse> cvs,
        LocalDate dateOfBirth,
        String gender,
        String address,
        String candidateSource,
        String linkedInUrl,
        String portfolioUrl) {
}
