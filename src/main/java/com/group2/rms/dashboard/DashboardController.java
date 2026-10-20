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
        // dunglt bổ sung: ứng viên không còn dashboard riêng, mọi thứ ở /portal/applications (đơn, lịch, thư mời).
        // Chặn trước dashboardService nên không chạy truy vấn dashboard ứng viên (đang lỗi ép kiểu InterviewFormat).
        if (isCandidate(authentication)) {
            return "redirect:/portal/applications";
        }
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

    private static boolean isCandidate(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CANDIDATE".equals(authority.getAuthority()));
    }
}
