package com.group2.rms.interview.controller;

import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.interview.service.InterviewResultService;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Interview Result Details (SRS 5.1.28): xem phiếu đánh giá và kết quả của một buổi phỏng vấn.
 * Quyền xem chi tiết (D5, GBR-05) nằm ở InterviewResultService.
 */
@Controller
@RequestMapping("/interviews")
@RequiredArgsConstructor
public class InterviewResultController {

    private final InterviewResultService interviewResultService;
    private final CurrentUserService currentUserService;

    @GetMapping("/{id}/result")
    public String viewResult(@PathVariable("id") Long id, Model model) {
        User viewer = currentUserService.requireUser();
        model.addAttribute("result", interviewResultService.getResult(id, viewer));
        model.addAttribute("activeMenu", "interviews");
        return "interview/result";
    }
}
