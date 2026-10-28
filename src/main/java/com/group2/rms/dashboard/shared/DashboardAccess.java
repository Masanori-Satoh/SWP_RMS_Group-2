package com.group2.rms.dashboard.shared;

import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Set;

/*
 * ACCESS: ai được vào dashboard nào (Admin, nội bộ, ứng viên) và người đang xem có còn Active không.
 */
@Component
public class DashboardAccess {

    public static final String ROLE_SYSTEM_ADMIN = "System Admin";
    public static final String ROLE_HR = "HR";
    public static final String ROLE_HIRING_MANAGER = "Hiring Manager";
    public static final String ROLE_DIRECTOR = "Director";
    public static final String ROLE_INTERVIEWER = "Interviewer";
    public static final String ROLE_CANDIDATE = "Candidate";

    public static final String ADMIN_DASHBOARD_URL = "/dashboard/admin";
    public static final String INTERNAL_DASHBOARD_URL = "/dashboard/internal";
    /** Ứng viên không có dashboard riêng: mọi thứ nằm ở trang đơn ứng tuyển. */
    public static final String CANDIDATE_HOME_URL = "/portal/applications";

    private static final Set<String> INTERNAL_ROLES = Set.of(
            ROLE_HR, ROLE_HIRING_MANAGER, ROLE_DIRECTOR, ROLE_INTERVIEWER);

    private final UserRepository userRepository;

    public DashboardAccess(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Người đang xem dashboard: phải có tài khoản và tài khoản còn Active. */
    public User actor(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        if (!"Active".equals(user.getAccountStatus())) {
            throw new AccessDeniedException("Inactive account cannot access dashboard");
        }
        return user;
    }

    public String role(User user) {
        if (user == null || user.getRole() == null) {
            return "";
        }
        return user.getRole().getRoleName();
    }

    /** Trang mà /dashboard chuyển tới, tùy theo role. */
    public String landingUrl(User user) {
        String role = role(user);
        if (ROLE_SYSTEM_ADMIN.equals(role)) {
            return ADMIN_DASHBOARD_URL;
        }
        if (ROLE_CANDIDATE.equals(role)) {
            return CANDIDATE_HOME_URL;
        }
        if (INTERNAL_ROLES.contains(role)) {
            return INTERNAL_DASHBOARD_URL;
        }
        throw new IllegalStateException("Unsupported dashboard role");
    }

    public void requireAdmin(User user) {
        if (!ROLE_SYSTEM_ADMIN.equals(role(user))) {
            throw new AccessDeniedException("Only System Admin can open the admin dashboard");
        }
    }

    public void requireInternal(User user) {
        if (!INTERNAL_ROLES.contains(role(user))) {
            throw new AccessDeniedException("Only internal staff can open the internal dashboard");
        }
    }
}
