package com.group2.rms.dashboard.admin;

import com.group2.rms.admin.dto.MonitorRowResponse;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.dashboard.admin.controller.AdminDashboardController;
import com.group2.rms.dashboard.admin.service.AdminDashboardService;
import com.group2.rms.dashboard.admin.dto.AdminDashboardResponse;
import com.group2.rms.dashboard.admin.dto.AdminDashboardResponse.*;
import com.group2.rms.dashboard.shared.DashboardMetricsRepository;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real dashboard/layout rendering with fixture data; no database or service probes. */
@WebMvcTest(AdminDashboardController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class AdminDashboardViewTests {
    @Autowired MockMvc mvc;
    @MockitoBean AdminDashboardService dashboards;
    @MockitoBean UserRepository users;

    @BeforeEach void activeAdminSession() {
        when(users.findByUsernameIgnoreCase("dashboard-ui")).thenReturn(Optional.of(User.builder()
                .userId(1).username("dashboard-ui").fullName("Quản trị viên").email("admin@example.test")
                .accountStatus("Active").role(Role.builder().roleId(73).roleName("System Admin").build()).build()));
    }

    private AdminDashboardResponse fixture(boolean empty, List<MonitorRowResponse> monitorRows) {
        var to = LocalDateTime.of(2026, 10, 23, 10, 30);
        return new AdminDashboardResponse("System Admin", "Quản trị viên", "Quản lý tài khoản và theo dõi dịch vụ hệ thống",
                List.of(new AccountSummary("Tài khoản nội bộ", "/admin/accounts", empty ? 0 : 12, empty ? 0 : 10, empty ? 0 : 2),
                        new AccountSummary("Tài khoản ứng viên", "/admin/candidate-accounts", empty ? 0 : 192, empty ? 0 : 180, empty ? 0 : 12)),
                new DepartmentSummary(0, 0, 0), List.of(), List.of(),
                List.of(new DashboardMetricsRepository.RoleCount(73, "System Admin", empty ? 0 : 1),
                        new DashboardMetricsRepository.RoleCount(74, "HR", empty ? 0 : 2),
                        new DashboardMetricsRepository.RoleCount(75, "Hiring Manager", empty ? 0 : 3),
                        new DashboardMetricsRepository.RoleCount(76, "Director", empty ? 0 : 1),
                        new DashboardMetricsRepository.RoleCount(77, "Interviewer", empty ? 0 : 5)),
                monitorRows, new CandidateSummary(empty ? 0 : 192, empty ? 0 : 57, to.minusDays(30), to, "Asia/Saigon"));
    }

    private String render(AdminDashboardResponse data, String snapshot) throws Exception {
        when(dashboards.forUsername("dashboard-ui")).thenReturn(data);
        String html = mvc.perform(get("/dashboard/admin").with(user("dashboard-ui").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        if (Boolean.getBoolean("dashboard.ui.snapshots")) {
            Files.createDirectories(Path.of("target/dashboard-ui"));
            Files.writeString(Path.of("target/dashboard-ui/" + snapshot + ".html"), html, StandardCharsets.UTF_8);
        }
        return html;
    }

    @Test void keepsRealTotalsFilterDestinationsAndThreeMonitorResults() throws Exception {
        var at = LocalDateTime.of(2026, 10, 23, 10, 30);
        String html = render(fixture(false, List.of(
                new MonitorRowResponse("internal", "SQL Server", "internal", "OPERATIONAL", 95L, at),
                new MonitorRowResponse("email", "Email", "external", "FAILED", 120L, at),
                new MonitorRowResponse("ai", "AI", "external", "Unconfigured", null, null))), "populated");
        for (String state : List.of("Active", "Inactive")) {
            assertTrue(html.contains("/admin/accounts?status=" + state + "&amp;page=0"));
            assertTrue(html.contains("/admin/candidate-accounts?status=" + state + "&amp;page=0"));
        }
        assertFalse(html.contains("status=Blocked"));
        assertFalse(html.contains("data-key=\"Blocked\""));
        assertFalse(html.contains("Bị khóa"));
        for (int id : List.of(73, 74, 75, 76, 77)) assertTrue(html.contains("roleId=" + id + "&amp;page=0"));
        assertTrue(html.contains("95 ms"));
        assertTrue(html.contains("23/10/2026 10:30:00"));
        assertTrue(html.contains("Hoạt động tốt"));
        assertTrue(html.contains("Thất bại"));
        assertTrue(html.contains("Chưa cấu hình"));
        assertTrue(html.contains("/admin/api-monitoring"));
        assertFalse(html.contains("/admin/accounts/new"));
        assertFalse(html.contains("30 ngày gần đây"));
        assertTrue(html.contains("Chưa lọc 30 ngày"));
        assertFalse(html.contains("99.98%"));
        assertFalse(html.contains("Nhật ký hoạt động gần đây"));
    }

    @Test void missingMeasurementsStayUnknownRatherThanZeroOrHealthy() throws Exception {
        String html = render(fixture(false, List.of(
                new MonitorRowResponse("internal", "SQL Server", "internal", "UNKNOWN", null, null),
                new MonitorRowResponse("email", "Email", "external", "Unconfigured", null, null))), "unknown");
        assertTrue(html.contains("Chưa có kết quả"));
        assertTrue(html.contains("Chưa kiểm tra"));
        assertTrue(html.contains("Chưa cấu hình SMTP"));
        assertFalse(html.contains("Hoạt động tốt"));
        assertFalse(html.contains(">0 ms<"));
    }

    @Test void emptyDashboardPreservesZeroRolesAndFallbackNavigation() throws Exception {
        String html = render(fixture(true, List.of()), "empty");
        assertTrue(html.contains("data-count=\"0\""));
        assertTrue(html.contains("Nhóm này chưa có tài khoản"));
        assertTrue(html.contains("Chưa có dữ liệu giám sát"));
        assertTrue(html.contains("/admin/accounts?roleId=77&amp;page=0"));
        assertFalse(html.contains("class=\"table adm-service-table\""));
    }
}
