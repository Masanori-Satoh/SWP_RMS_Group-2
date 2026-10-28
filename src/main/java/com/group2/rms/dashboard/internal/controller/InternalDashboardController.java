package com.group2.rms.dashboard.internal.controller;

import com.group2.rms.dashboard.internal.dto.DashboardResponse;
import com.group2.rms.dashboard.internal.service.InternalDashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/*
 * CONTROLLER: trang dashboard nội bộ (/dashboard/internal) cho HR, Hiring Manager, Director, Interviewer.
 */
@Controller
public class InternalDashboardController {

    private final InternalDashboardService internalDashboardService;

    public InternalDashboardController(InternalDashboardService internalDashboardService) {
        this.internalDashboardService = internalDashboardService;
    }

    /** week: tuần của "Lịch tuần này" (Interviewer), giữ dạng chuỗi để giá trị lạ quay về tuần hiện tại (D13). */
    @GetMapping("/dashboard/internal")
    public String dashboard(@RequestParam(value = "week", required = false) String week,
            Authentication authentication, Model model) {
        DashboardResponse dashboard = internalDashboardService.forUsername(authentication.getName(), week);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("pageLang", "vi");
        return "dashboard/index";
    }
}
