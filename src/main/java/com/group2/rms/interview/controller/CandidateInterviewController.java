package com.group2.rms.interview.controller;

import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.interview.service.InterviewSchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * "Lịch phỏng vấn của tôi" của ứng viên (menu sidebar candidate trỏ tới /portal/interviews).
 * SecurityConfig đã giới hạn GET /portal/interviews cho ROLE_CANDIDATE; ứng viên chỉ thấy lịch của hồ sơ mình.
 */
@Controller
@RequestMapping("/portal/interviews")
@RequiredArgsConstructor
public class CandidateInterviewController {

    private final InterviewSchedulingService interviewSchedulingService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String myInterviews(Model model) {
        Integer candidateUserId = currentUserService.requireUser().getUserId();
        model.addAttribute("schedules", interviewSchedulingService.getSchedulesForCandidate(candidateUserId));
        model.addAttribute("activeMenu", "interviews");
        return "interview/candidate-list";
    }
}
