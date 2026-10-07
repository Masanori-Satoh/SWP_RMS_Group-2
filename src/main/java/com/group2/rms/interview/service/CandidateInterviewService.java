package com.group2.rms.interview.service;

import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.interview.dto.CandidateInterviewResponse;
import com.group2.rms.interview.repository.InterviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CandidateInterviewService {
    private final CurrentUserService currentUser;
    private final InterviewScheduleRepository schedules;

    public List<CandidateInterviewResponse> getMyInterviews() {
        var user = currentUser.requireUser();
        if (!CurrentUserService.hasRole(user, "Candidate")) {
            throw new AccessDeniedException("Candidate access is required.");
        }
        return schedules.findAllByCandidateUserId(user.getUserId());
    }
}
