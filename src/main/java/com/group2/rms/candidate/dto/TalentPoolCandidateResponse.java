package com.group2.rms.candidate.dto;

import java.util.List;

// get potential candidate
public record TalentPoolCandidateResponse(
        Integer candidateId,
        String fullName,
        String email,
        String phoneNumber,
        List<TalentPoolCvResponse> cvs) {
}
