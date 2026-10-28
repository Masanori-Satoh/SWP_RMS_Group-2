package com.group2.rms.dashboard;

import com.group2.rms.dashboard.shared.DashboardAccess;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * CONTROLLER: cửa vào /dashboard, chuyển người dùng tới dashboard đúng với role (Admin, nội bộ, ứng viên).
 */
@Controller
public class DashboardEntryController {

    private final DashboardAccess access;

    public DashboardEntryController(DashboardAccess access) {
        this.access = access;
    }

    @GetMapping({"/dashboard", "/dashboard/"})
    public String dashboard(Authentication authentication) {
        String landingUrl = access.landingUrl(access.actor(authentication.getName()));
        return "redirect:" + landingUrl;
    }
}
