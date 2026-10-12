package com.group2.rms.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.group2.rms.dashboard.dto.DashboardResponse;
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
        DashboardResponse dashboard = dashboardService.forUsername(authentication.getName());
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("pageLang", "vi");
        return switch (dashboard.roleName()) {
            case "System Admin" -> "dashboard/admin";
            case "Candidate" -> "dashboard/candidate";
            case "HR", "Hiring Manager", "Director", "Interviewer" -> "dashboard/index";
            default -> throw new IllegalStateException(
                    "Unsupported dashboard role");
        };
    }
}
