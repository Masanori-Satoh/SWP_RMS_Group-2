package com.group2.rms.dashboard.admin.controller;

import com.group2.rms.dashboard.admin.service.AdminDashboardService;
import com.group2.rms.admin.service.AuditLogService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * CONTROLLER: trang dashboard System Admin (/dashboard/admin).
 */
@Controller
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AuditLogService audit;

    public AdminDashboardController(AdminDashboardService adminDashboardService, AuditLogService audit) {
        this.adminDashboardService = adminDashboardService;
        this.audit = audit;
    }

    @GetMapping("/dashboard/admin")
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("dashboard", adminDashboardService.forUsername(authentication.getName()));
        model.addAttribute("recentAuditLogs", audit.recent());
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("pageLang", "vi");
        return "dashboard/admin";
    }
}
