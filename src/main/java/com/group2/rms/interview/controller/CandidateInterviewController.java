package com.group2.rms.interview.controller;

import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.interview.service.CandidateInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/portal/interviews")
@RequiredArgsConstructor
public class CandidateInterviewController {
    private final CandidateInterviewService interviews;
    private final CurrentUserService currentUser;

    @GetMapping
    public String listInterviews(Model model) {
        model.addAttribute("schedules", interviews.getMyInterviews());
        model.addAttribute("currentUser", currentUser.requireUser());
        return "interview/candidate-list";
    }
}
