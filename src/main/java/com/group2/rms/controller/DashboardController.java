package com.group2.rms.controller;

import com.group2.rms.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Authenticated landing page with role-scoped dashboard data. */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("dashboard", dashboardService.forUsername(authentication.getName()));
        return "dashboard/index";
    }
}
