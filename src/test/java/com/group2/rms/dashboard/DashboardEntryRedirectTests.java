package com.group2.rms.dashboard;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.core.security.RoleAuthorities;
import com.group2.rms.dashboard.shared.DashboardAccess;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * WEB TEST: /dashboard chuyển Admin sang /dashboard/admin, nhân viên sang /dashboard/internal, ứng viên sang /portal/applications.
 */
@WebMvcTest(DashboardEntryController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class, DashboardAccess.class})
class DashboardEntryRedirectTests {

    @Autowired MockMvc mvc;
    @Autowired DashboardAccess access;
    @MockitoBean UserRepository users;

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "System Admin, /dashboard/admin",
            "HR, /dashboard/internal",
            "Hiring Manager, /dashboard/internal",
            "Director, /dashboard/internal",
            "Interviewer, /dashboard/internal",
            "Candidate, /portal/applications"
    })
    @DisplayName("DASHBOARD-ENTRY-01 | /dashboard chuyển tới đúng dashboard theo role")
    void dashboard_shouldRedirectToRoleDashboard_whenAccountActive(String roleName, String expectedUrl) throws Exception {
        // Given
        when(users.findByUsernameIgnoreCase("viewer")).thenReturn(Optional.of(account(roleName)));

        // When / Then
        mvc.perform(get("/dashboard").with(user("viewer")
                        .authorities(() -> RoleAuthorities.fromRoleName(roleName))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedUrl));
    }

    @Test
    @DisplayName("DASHBOARD-ENTRY-02 | Role lạ: không đoán dashboard")
    void landingUrl_shouldThrow_whenRoleUnsupported() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> access.landingUrl(account("Unknown")));

        assertEquals("Unsupported dashboard role", error.getMessage());
    }

    private User account(String roleName) {
        return User.builder().userId(1).username("viewer").fullName("Viewer").accountStatus("Active")
                .role(Role.builder().roleName(roleName).build()).build();
    }
}
