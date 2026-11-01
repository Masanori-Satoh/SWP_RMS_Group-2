package com.group2.rms.dashboard.internal.controller;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.core.security.RoleAuthorities;
import com.group2.rms.dashboard.internal.dto.HrDashboardResponse;
import com.group2.rms.dashboard.internal.dto.InterviewerDashboardResponse;
import com.group2.rms.dashboard.internal.service.DirectorDashboardService;
import com.group2.rms.dashboard.internal.service.HiringManagerDashboardService;
import com.group2.rms.dashboard.internal.service.HrDashboardService;
import com.group2.rms.dashboard.internal.service.InternalDashboardService;
import com.group2.rms.dashboard.internal.service.InterviewerDashboardService;
import com.group2.rms.dashboard.shared.DashboardAccess;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * WEB TEST: InternalDashboardController với bảo mật thật; InternalDashboardService + DashboardAccess là bản thật
 * để kiểm được khâu chặn role, 4 service theo role và UserRepository là mock (không cần DB).
 */
@WebMvcTest(InternalDashboardController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class, DashboardAccess.class, InternalDashboardService.class})
class InternalDashboardControllerTests {

    private static final String USERNAME = "viewer";

    @Autowired MockMvc mvc;
    @MockitoBean UserRepository users;
    @MockitoBean HrDashboardService hrDashboard;
    @MockitoBean HiringManagerDashboardService hiringManagerDashboard;
    @MockitoBean DirectorDashboardService directorDashboard;
    @MockitoBean InterviewerDashboardService interviewerDashboard;

    @Test
    @DisplayName("Chưa đăng nhập gõ /dashboard/internal -> Chuyển về trang đăng nhập, không dựng dashboard")
    void dashboard_shouldRedirectToLogin_whenAnonymous() throws Exception {
        // Act
        mvc.perform(get(DashboardAccess.INTERNAL_DASHBOARD_URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Assert
        verifyNoRoleDashboardBuilt();
    }

    @ParameterizedTest(name = "{0} -> 403")
    @ValueSource(strings = {DashboardAccess.ROLE_CANDIDATE, DashboardAccess.ROLE_SYSTEM_ADMIN})
    @DisplayName("Role không phải nhân viên nội bộ gõ thẳng URL -> 403, không dựng dashboard")
    void dashboard_shouldReturnForbidden_whenRoleIsNotInternal(String roleName) throws Exception {
        // Arrange
        when(users.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(account(roleName, "Active")));

        // Act
        mvc.perform(get(DashboardAccess.INTERNAL_DASHBOARD_URL).with(loginAs(roleName)))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));

        // Assert
        verifyNoRoleDashboardBuilt();
    }

    @Test
    @DisplayName("Tài khoản bị khóa giữa phiên -> Đăng xuất, về /login?session-expired, không dựng dashboard")
    void dashboard_shouldLogOut_whenAccountBlocked() throws Exception {
        // Arrange
        when(users.findByUsernameIgnoreCase(USERNAME))
                .thenReturn(Optional.of(account(DashboardAccess.ROLE_HR, "Blocked")));

        // Act
        mvc.perform(get(DashboardAccess.INTERNAL_DASHBOARD_URL).with(loginAs(DashboardAccess.ROLE_HR)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?session-expired"));

        // Assert
        verifyNoRoleDashboardBuilt();
    }

    @Test
    @DisplayName("HR mở dashboard -> 200, trang dashboard/index, chỉ gọi service của HR")
    void dashboard_shouldRenderHrDashboard_whenHrLogsIn() throws Exception {
        // Arrange
        User hr = account(DashboardAccess.ROLE_HR, "Active");
        HrDashboardResponse response = new HrDashboardResponse(DashboardAccess.ROLE_HR, "Viewer",
                List.of(), List.of(), 0, List.of(), List.of());
        when(users.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(hr));
        when(hrDashboard.build(any(User.class))).thenReturn(response);

        // Act
        mvc.perform(get(DashboardAccess.INTERNAL_DASHBOARD_URL).with(loginAs(DashboardAccess.ROLE_HR)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(model().attribute("dashboard", response))
                .andExpect(model().attribute("activeMenu", "dashboard"));

        // Assert
        verify(hrDashboard).build(any(User.class));
        verifyNoInteractions(hiringManagerDashboard, directorDashboard, interviewerDashboard);
    }

    @Test
    @DisplayName("Interviewer mở ?week=abc -> Chuỗi week được chuyển nguyên cho service của Interviewer")
    void dashboard_shouldPassRawWeekToInterviewerService_whenWeekParameterGiven() throws Exception {
        // Arrange
        InterviewerDashboardResponse response = new InterviewerDashboardResponse(DashboardAccess.ROLE_INTERVIEWER,
                "Viewer", List.of(), List.of(), List.of(), LocalDate.of(2026, 10, 26));
        when(users.findByUsernameIgnoreCase(USERNAME))
                .thenReturn(Optional.of(account(DashboardAccess.ROLE_INTERVIEWER, "Active")));
        when(interviewerDashboard.build(any(User.class), eq("abc"))).thenReturn(response);

        // Act
        mvc.perform(get(DashboardAccess.INTERNAL_DASHBOARD_URL).param("week", "abc").with(loginAs(DashboardAccess.ROLE_INTERVIEWER)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("dashboard", response));

        // Assert
        verify(interviewerDashboard).build(any(User.class), eq("abc"));
        verifyNoInteractions(hrDashboard, hiringManagerDashboard, directorDashboard);
    }

    // ----- Hàm hỗ trợ -----

    private RequestPostProcessor loginAs(String roleName) {
        return user(USERNAME).authorities(() -> RoleAuthorities.fromRoleName(roleName));
    }

    private User account(String roleName, String status) {
        return User.builder().userId(1).username(USERNAME).fullName("Viewer").accountStatus(status)
                .role(Role.builder().roleName(roleName).build()).build();
    }

    private void verifyNoRoleDashboardBuilt() {
        verifyNoInteractions(hrDashboard, hiringManagerDashboard, directorDashboard, interviewerDashboard);
    }
}
