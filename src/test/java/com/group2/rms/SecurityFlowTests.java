package com.group2.rms;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.auth.AuthController;
import com.group2.rms.user.controller.AccountController;
import com.group2.rms.admin.ApiMonitoringController;
import com.group2.rms.dashboard.DashboardController;
import com.group2.rms.admin.HealthController;
import com.group2.rms.auth.PasswordRecoveryController;
import com.group2.rms.auth.RegistrationController;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.dashboard.DashboardMetricsRepository.ApprovalActivity;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.service.AccountListService;
import com.group2.rms.user.service.AccountManagementService;
import com.group2.rms.admin.ApiMonitoringService;
import com.group2.rms.dashboard.DashboardService;
import com.group2.rms.dashboard.DashboardView;
import com.group2.rms.auth.CandidateRegistrationService;
import com.group2.rms.auth.PasswordResetEmailSender;
import com.group2.rms.auth.PasswordResetService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@WebMvcTest(controllers = {AuthController.class, DashboardController.class, AccountController.class,
        ApiMonitoringController.class, HealthController.class,
        RegistrationController.class, PasswordRecoveryController.class})
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class SecurityFlowTests {

    @Autowired private MockMvc mvc;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private AccountListService accountListService;
    @MockitoBean private AccountManagementService accountManagementService;
    @MockitoBean private DashboardService dashboardService;
    @MockitoBean private ApiMonitoringService monitoringService;
    @MockitoBean private JdbcTemplate jdbcTemplate;
    @MockitoBean private CandidateRegistrationService registrationService;
    @MockitoBean private PasswordResetService passwordResetService;
    @MockitoBean private PasswordResetEmailSender passwordResetEmailSender;

    @Test
    void guestsNeedAuthenticationAndLoginNeedsCsrf() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/admin/accounts")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(post("/login").param("username", "admin").param("password", "test-password"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginShowsRegisterAndForgotPasswordAndRegistrationRequiresCsrf() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/register\"")))
                .andExpect(content().string(containsString("href=\"/forgot-password\"")));
        mvc.perform(post("/register")).andExpect(status().isForbidden());

        MvcResult page = mvc.perform(get("/register")).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"username\"")))
                .andReturn();
        CsrfToken csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/register")
                        .session((MockHttpSession) page.getRequest().getSession(false))
                        .param(csrf.getParameterName(), csrf.getToken())
                        .param("fullName", "Ứng viên A")
                        .param("username", "candidateA")
                        .param("email", "candidate@example.test")
                        .param("password", "validPassword12")
                        .param("confirmPassword", "validPassword12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
        verify(registrationService).register(new CandidateRegistrationService.RegisterCommand(
                "Ứng viên A", "candidateA", "candidate@example.test", "validPassword12"));
    }

    @Test
    void passwordRecoveryRequiresCsrfAndRendersGenericConfirmation() throws Exception {
        when(passwordResetService.isConfigured()).thenReturn(true);
        when(passwordResetEmailSender.isConfigured()).thenReturn(true);
        when(passwordResetService.request("candidate@example.test")).thenReturn(
                Optional.of(new PasswordResetService.ResetLink("candidate@example.test", "signed-token")));
        mvc.perform(post("/forgot-password")).andExpect(status().isForbidden());

        MvcResult page = mvc.perform(get("/forgot-password")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Gửi liên kết xác minh")))
                .andReturn();
        CsrfToken csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/forgot-password")
                        .session((MockHttpSession) page.getRequest().getSession(false))
                        .param(csrf.getParameterName(), csrf.getToken())
                        .param("email", "candidate@example.test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password?sent"));
        verify(passwordResetEmailSender).send("candidate@example.test", "signed-token");

        mvc.perform(post("/forgot-password")
                        .session((MockHttpSession) page.getRequest().getSession(false))
                        .param(csrf.getParameterName(), csrf.getToken())
                        .param("email", "unknown@example.test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password?sent"));
    }

    @Test
    void signedResetLinkRendersFormAndChangesPasswordOnlyWithCsrf() throws Exception {
        String route = "/reset-password/signed-token";
        when(passwordResetService.isValid("signed-token")).thenReturn(true);
        when(passwordResetService.reset("signed-token", "newPassword12")).thenReturn(true);
        mvc.perform(post(route)).andExpect(status().isForbidden());

        MvcResult page = mvc.perform(get(route)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Mật khẩu mới")))
                .andReturn();
        CsrfToken csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post(route)
                        .session((MockHttpSession) page.getRequest().getSession(false))
                        .param(csrf.getParameterName(), csrf.getToken())
                        .param("password", "newPassword12")
                        .param("confirmPassword", "newPassword12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?reset"));
        verify(passwordResetService).reset("signed-token", "newPassword12");
    }

    @Test
    void adminCanEnterAdminRouteAndHrCannot() throws Exception {
        when(accountListService.findAccounts("", null, null, "", "name", 0)).thenReturn(Page.empty());
        User admin = account("admin", "System Admin", "Active");
        MockHttpSession adminSession = login(admin);
        mvc.perform(get("/dashboard").session(adminSession)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Quản lý tài khoản")))
                .andExpect(content().string(containsString("Cấu hình AI (chưa khả dụng)")));
        mvc.perform(get("/admin/accounts").session(adminSession)).andExpect(status().isOk());
        when(monitoringService.rows()).thenReturn(List.of(new ApiMonitoringService.MonitorRow(
                "internal", "Nội bộ", "Ứng dụng và SQL Server", "GET /admin/api-monitoring/internal/health",
                "OPERATIONAL", "Hoạt động", 12L, 0, 0, 200,
                LocalDateTime.of(2026, 9, 28, 9, 0), 1, true),
                new ApiMonitoringService.MonitorRow("ai", "Tích hợp ngoài", "AI CV Screening",
                        "Chưa có endpoint", "UNCONFIGURED", "Chưa cấu hình",
                        null, null, null, null, null, 0, false)));
        mvc.perform(get("/admin/api-monitoring").session(adminSession)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Gửi GET")))
                .andExpect(content().string(containsString("HTTP 200")))
                .andExpect(content().string(containsString("Chưa có target an toàn")));
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        mvc.perform(get("/admin/api-monitoring/internal/health").session(adminSession))
                .andExpect(status().isOk()).andExpect(content().string(containsString("UP")));
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new DataAccessResourceFailureException("private SQL details"));
        mvc.perform(get("/admin/api-monitoring/internal/health").session(adminSession))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(containsString("DOWN")))
                .andExpect(content().string(not(containsString("private SQL details"))));
        mvc.perform(get("/admin/ai-configuration").session(adminSession)).andExpect(status().isNotFound());

        User hr = account("hr", "HR", "Active");
        MockHttpSession hrSession = login(hr);
        mvc.perform(get("/dashboard").session(hrSession)).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Quản lý tài khoản"))));
        for (String route : new String[] {"/admin/api-monitoring", "/admin/api-monitoring/internal/health",
                "/admin/ai-configuration"}) {
            mvc.perform(get(route).session(hrSession)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/admin/accounts").session(hrSession)).andExpect(status().isForbidden());
    }

    @Test
    void monitoringProbeNeedsAdminAndCsrf() throws Exception {
        MockHttpSession adminSession = login(account("admin", "System Admin", "Active"));
        mvc.perform(post("/admin/api-monitoring/probe/internal").session(adminSession))
                .andExpect(status().isForbidden());

        MvcResult page = mvc.perform(get("/admin/api-monitoring").session(adminSession))
                .andExpect(status().isOk()).andReturn();
        CsrfToken csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        when(monitoringService.probeInternal(any())).thenReturn(
                new ApiMonitoringService.ProbeOutcome(true, "HTTP 200", 12));
        mvc.perform(post("/admin/api-monitoring/probe/internal").session(adminSession)
                        .param(csrf.getParameterName(), csrf.getToken()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/api-monitoring"));
        verify(monitoringService).probeInternal(any());

        MockHttpSession hrSession = login(account("hr", "HR", "Active"));
        mvc.perform(post("/admin/api-monitoring/probe/internal").session(hrSession)
                        .param(csrf.getParameterName(), csrf.getToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivateRequiresAdminAndCsrf() throws Exception {
        when(accountListService.findAccounts("", null, null, "", "name", 0)).thenReturn(Page.empty());
        MockHttpSession adminSession = login(account("admin", "System Admin", "Active"));
        mvc.perform(post("/admin/accounts/42/deactivate").session(adminSession))
                .andExpect(status().isForbidden());

        MvcResult list = mvc.perform(get("/admin/accounts").session(adminSession))
                .andExpect(status().isOk()).andReturn();
        CsrfToken csrf = (CsrfToken) list.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/admin/accounts/42/deactivate").session(adminSession)
                        .param(csrf.getParameterName(), csrf.getToken()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"));
        verify(accountListService).deactivate(42);

        MockHttpSession hrSession = login(account("hr", "HR", "Active"));
        mvc.perform(post("/admin/accounts/42/deactivate").session(hrSession)
                        .param(csrf.getParameterName(), csrf.getToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountListRendersCandidateAccountWithoutPasswordHash() throws Exception {
        when(accountListService.findAccounts("", null, null, "", "name", 0)).thenReturn(
                new PageImpl<>(List.of(new AccountListService.AccountRow(
                        7, "Ứng viên A", "candidate@example.com", "candidate7", "Candidate", null, "Active"))));
        MockHttpSession adminSession = login(account("admin", "System Admin", "Active"));
        mvc.perform(get("/admin/accounts").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ứng viên A")))
                .andExpect(content().string(containsString("Candidate")))
                .andExpect(content().string(containsString("/admin/accounts/7/deactivate")))
                .andExpect(content().string(not(containsString("test-password"))));
    }

    @Test
    void accountFormsRequireAdminAndRenderEditWithoutPasswordField() throws Exception {
        MockHttpSession adminSession = login(account("admin", "System Admin", "Active"));
        mvc.perform(get("/admin/accounts/new").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("name=\"password\"")));

        when(accountManagementService.findForEdit(42)).thenReturn(
                new AccountManagementService.AccountForEdit(
                        "stable42", "Người dùng 42", "user42@example.com", null, 1, 1, "Active"));
        mvc.perform(get("/admin/accounts/42/edit").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("stable42")))
                .andExpect(content().string(not(containsString("name=\"password\""))));

        MockHttpSession hrSession = login(account("hr", "HR", "Active"));
        mvc.perform(get("/admin/accounts/new").session(hrSession)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/accounts/42/edit").session(hrSession)).andExpect(status().isForbidden());
    }

    @Test
    void createFormValidatesConfirmationAndPostRequiresCsrf() throws Exception {
        MockHttpSession adminSession = login(account("admin", "System Admin", "Active"));
        mvc.perform(post("/admin/accounts").session(adminSession)).andExpect(status().isForbidden());

        MvcResult page = mvc.perform(get("/admin/accounts/new").session(adminSession))
                .andExpect(status().isOk()).andReturn();
        CsrfToken csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/admin/accounts").session(adminSession)
                        .param(csrf.getParameterName(), csrf.getToken())
                        .param("fullName", "Người mới")
                        .param("username", "newuser")
                        .param("email", "new@example.com")
                        .param("roleId", "2")
                        .param("departmentId", "1")
                        .param("password", "validPassword12")
                        .param("confirmPassword", "differentPassword"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mật khẩu xác nhận không khớp.")))
                .andExpect(content().string(not(containsString("value=\"validPassword12\""))));
        verifyNoInteractions(accountManagementService);
    }

    @Test
    void validCreateAndUpdateRedirectWithSuccess() throws Exception {
        MockHttpSession adminSession = login(account("admin", "System Admin", "Active"));
        MvcResult page = mvc.perform(get("/admin/accounts/new").session(adminSession))
                .andExpect(status().isOk()).andReturn();
        CsrfToken csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        AccountManagementService.CreateCommand create = new AccountManagementService.CreateCommand(
                "Người mới", "newuser", "new@example.com", "0900000000", 2, 1, "validPassword12");
        when(accountManagementService.create(create)).thenReturn(42);
        mvc.perform(post("/admin/accounts").session(adminSession)
                        .param(csrf.getParameterName(), csrf.getToken())
                        .param("fullName", "Người mới")
                        .param("username", "newuser")
                        .param("email", "new@example.com")
                        .param("phoneNumber", "0900000000")
                        .param("roleId", "2")
                        .param("departmentId", "1")
                        .param("password", "validPassword12")
                        .param("confirmPassword", "validPassword12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"));
        verify(accountManagementService).create(create);

        AccountManagementService.UpdateCommand update = new AccountManagementService.UpdateCommand(
                "Tên mới", "updated@example.com", null, 2, 1, "Inactive");
        mvc.perform(post("/admin/accounts/42").session(adminSession)
                        .param(csrf.getParameterName(), csrf.getToken())
                        .param("fullName", "Tên mới")
                        .param("email", "updated@example.com")
                        .param("roleId", "2")
                        .param("departmentId", "1")
                        .param("accountStatus", "Inactive")
                        .param("username", "tampered-username")
                        .param("password", "tampered-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"));
        verify(accountManagementService).update(42, update);
    }

    @Test
    void everyConfirmedRoleCanOpenDashboard() throws Exception {
        String[] roles = {"System Admin", "HR", "Hiring Manager", "Director", "Interviewer", "Candidate"};
        for (int index = 0; index < roles.length; index++) {
            MockHttpSession session = login(account("role" + index, roles[index], "Active"));
            mvc.perform(get("/dashboard").session(session)).andExpect(status().isOk());
        }
    }

    @Test
    void disabledAccountsCannotLoginAndExistingSessionIsRevoked() throws Exception {
        for (String status : new String[] {"Blocked", "Inactive"}) {
            User disabled = account(status.toLowerCase(), "HR", status);
            LoginForm form = loginForm();
            mvc.perform(post("/login").session(form.session())
                            .param(form.csrf().getParameterName(), form.csrf().getToken())
                            .param("username", disabled.getUsername()).param("password", "test-password"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
        }

        User active = account("active", "HR", "Active");
        MockHttpSession session = login(active);
        active.setAccountStatus("Inactive");
        mvc.perform(get("/dashboard").session(session)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?session-expired"));

        User changedRole = account("changed", "HR", "Active");
        MockHttpSession changedSession = login(changedRole);
        changedRole.setRole(Role.builder().roleName("Interviewer").build());
        mvc.perform(get("/dashboard").session(changedSession)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?session-expired"));
    }

    @Test
    void logoutRequiresCsrf() throws Exception {
        MockHttpSession session = login(account("admin", "System Admin", "Active"));
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        MvcResult dashboard = mvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk()).andReturn();
        CsrfToken csrf = (CsrfToken) dashboard.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/logout").session(session)
                        .param(csrf.getParameterName(), csrf.getToken()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    private User account(String username, String roleName, String status) {
        User account = User.builder()
                .userId(username.hashCode())
                .username(username)
                .passwordHash(passwordEncoder.encode("test-password"))
                .role(Role.builder().roleName(roleName).build())
                .accountStatus(status)
                .build();
        when(userRepository.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(account));
        when(dashboardService.forUsername(username)).thenReturn(new DashboardView(
                roleName, username, "Phạm vi thử nghiệm",
                List.of(new DashboardView.Metric("Chỉ số thử nghiệm", 0, "Dữ liệu kiểm thử")),
                List.of(),
                "Director".equals(roleName)
                        ? List.of(new ApprovalActivity("Yêu cầu tuyển dụng", "Vị trí kiểm thử", "Approved",
                                LocalDateTime.of(2026, 9, 28, 9, 0)))
                        : List.of(),
                List.of(),
                "System Admin".equals(roleName)
                        ? List.of(new DashboardView.Shortcut("Quản lý tài khoản", "/admin/accounts"),
                                new DashboardView.Shortcut("Cấu hình AI (chưa khả dụng)", null))
                        : List.of()));
        return account;
    }

    private MockHttpSession login(User account) throws Exception {
        LoginForm form = loginForm();
        HttpSession session = form.session();
        MvcResult result = mvc.perform(post("/login").session((MockHttpSession) session)
                        .param(form.csrf().getParameterName(), form.csrf().getToken())
                        .param("username", account.getUsername())
                        .param("password", "test-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private LoginForm loginForm() throws Exception {
        MvcResult result = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        CsrfToken csrf = (CsrfToken) result.getRequest().getAttribute(CsrfToken.class.getName());
        return new LoginForm((MockHttpSession) result.getRequest().getSession(false), csrf);
    }

    private record LoginForm(MockHttpSession session, CsrfToken csrf) {
    }
}
