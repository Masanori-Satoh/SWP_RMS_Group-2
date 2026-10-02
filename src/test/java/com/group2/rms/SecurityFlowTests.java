package com.group2.rms;

<<<<<<< Updated upstream
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
=======
import com.group2.rms.config.SecurityConfig;
import com.group2.rms.controller.AuthController;
import com.group2.rms.controller.AccountController;
import com.group2.rms.controller.CandidateAccountController;
import com.group2.rms.controller.ApiMonitoringController;
import com.group2.rms.controller.DashboardAccessController;
import com.group2.rms.controller.InternalMonitoringHealthController;
import com.group2.rms.controller.PasswordRecoveryController;
import com.group2.rms.controller.RegistrationController;
import com.group2.rms.entity.Role;
import com.group2.rms.entity.Department;
import com.group2.rms.entity.User;
import com.group2.rms.repository.UserRepository;
import com.group2.rms.repository.DashboardMetricsRepository.ApprovalActivity;
import com.group2.rms.security.DatabaseUserDetailsService;
import com.group2.rms.service.AccountListService;
import com.group2.rms.service.AccountManagementService;
import com.group2.rms.service.ApiMonitoringService;
import com.group2.rms.service.DashboardService;
import com.group2.rms.service.DashboardView;
import com.group2.rms.service.CandidateRegistrationService;
import com.group2.rms.service.PasswordResetEmailSender;
import com.group2.rms.service.PasswordResetService;
>>>>>>> Stashed changes
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

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

<<<<<<< Updated upstream
@WebMvcTest(controllers = {AuthController.class, DashboardController.class, AccountController.class,
        ApiMonitoringController.class, HealthController.class,
=======
@WebMvcTest(controllers = {AuthController.class, DashboardAccessController.class, AccountController.class, CandidateAccountController.class,
        ApiMonitoringController.class, InternalMonitoringHealthController.class,
>>>>>>> Stashed changes
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
                .andExpect(content().string(containsString("Send Verification Link")))
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
                .andExpect(content().string(containsString("New Password")))
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
                .andExpect(content().string(containsString("Internal Accounts")))
                .andExpect(content().string(containsString("AI Configuration (Unavailable)")));
        mvc.perform(get("/admin/accounts").session(adminSession)).andExpect(status().isOk());
        when(monitoringService.rows()).thenReturn(List.of(new ApiMonitoringService.MonitorRow(
                "internal", "Internal", "Application and SQL Server", "GET /admin/api-monitoring/internal/health",
                "OPERATIONAL", "Operational", 12L, 0, 0, 200,
                LocalDateTime.of(2026, 9, 28, 9, 0), 1, true),
                new ApiMonitoringService.MonitorRow("ai", "External Integration", "AI CV Screening",
                        "No endpoint configured", "UNCONFIGURED", "Not Configured",
                        null, null, null, null, null, 0, false)));
        mvc.perform(get("/admin/api-monitoring").session(adminSession)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Send GET")))
                .andExpect(content().string(containsString("HTTP 200")))
                .andExpect(content().string(containsString("No confirmed safe target")));
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
                .andExpect(content().string(not(containsString("Internal Accounts"))));
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
        when(accountListService.findCandidateAccounts("", "", "name", 0)).thenReturn(
                new PageImpl<>(List.of(new AccountListService.CandidateRow(
                        7, "Ứng viên A", "candidate@example.com", "candidate7", "Active", LocalDateTime.of(2026, 9, 1, 8, 0), 17),
                        new AccountListService.CandidateRow(8, "Taylor Candidate", "taylorcandidate@example.test", "candidate8", "Inactive", LocalDateTime.of(2026, 9, 2, 8, 0), 18),
                        new AccountListService.CandidateRow(9, "Jordan Candidate", "jordancandidate@example.test", "candidate9", "Blocked", LocalDateTime.of(2026, 9, 3, 8, 0), null))));
        MockHttpSession adminSession = login(account("admin", "System Admin", "Active"));
        MvcResult candidatePage = mvc.perform(get("/admin/candidate-accounts").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ứng viên A")))
                .andExpect(content().string(containsString("Candidate")))
                .andExpect(content().string(containsString("/admin/candidate-accounts/7/deactivate")))
                .andExpect(content().string(containsString("Linked · #17")))
                .andExpect(content().string(containsString("Profile missing")))
                .andExpect(content().string(not(containsString("/admin/candidate-accounts/8/deactivate"))))
                .andExpect(content().string(not(containsString("name=\"departmentId\""))))
                .andExpect(content().string(not(containsString("name=\"roleId\""))))
                .andExpect(content().string(not(containsString("<th scope=\"col\">Department"))))
                .andExpect(content().string(not(containsString("test-password")))).andReturn();
        exportUi("candidate-accounts", candidatePage);
        CsrfToken token = (CsrfToken) candidatePage.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/admin/candidate-accounts/7/deactivate").session(adminSession)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/candidate-accounts/7/deactivate").session(adminSession).param(token.getParameterName(), token.getToken()))
                .andExpect(redirectedUrl("/admin/candidate-accounts"));
        verify(accountListService).deactivateCandidate(7);
        for (String role : List.of("HR", "Candidate")) {
            MockHttpSession other = login(account("other" + role, role, "Active"));
            mvc.perform(get("/admin/candidate-accounts").session(other)).andExpect(status().isForbidden());
            mvc.perform(post("/admin/candidate-accounts/7/deactivate").session(other).param(token.getParameterName(), token.getToken()))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/admin/candidate-accounts")).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void internalFormCannotSubmitCandidateRoleOrEditCandidateIdentity() throws Exception {
        prepareUiOptions();
        MockHttpSession session = login(account("admin", "System Admin", "Active"));
        MvcResult page = mvc.perform(get("/admin/accounts/new").session(session))
                .andExpect(content().string(not(containsString("data-role-name=\"Candidate\"")))).andReturn();
        CsrfToken token = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        org.mockito.Mockito.doThrow(new com.group2.rms.service.AccountFieldException("roleId", "Select an internal role."))
                .when(accountManagementService).createInternal(any());
        mvc.perform(post("/admin/accounts").session(session).param(token.getParameterName(), token.getToken())
                .param("fullName", "Test Candidate").param("username", "tampered").param("email", "tampered@example.test")
                .param("roleId", "6").param("password", "validPassword12").param("confirmPassword", "validPassword12"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Select an internal role.")));
        org.mockito.Mockito.doThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND))
                .when(accountManagementService).findForEdit(7);
        mvc.perform(get("/admin/accounts/7/edit").session(session)).andExpect(status().isNotFound());
        mvc.perform(post("/admin/accounts/7").session(session).param(token.getParameterName(), token.getToken()).param("roleId", "2"))
                .andExpect(status().isNotFound());
        org.mockito.Mockito.verify(accountManagementService, org.mockito.Mockito.never()).updateInternal(org.mockito.ArgumentMatchers.eq(7), any());
    }

    @Test
    void accountListOffersDeactivationOnlyForActiveOrBlockedAccounts() throws Exception {
        when(accountListService.findAccounts("", null, null, "", "name", 0)).thenReturn(
                new PageImpl<>(List.of(
                        new AccountListService.AccountRow(41, "Active Employee", "active@example.test", "active41", "HR", "People", "Active"),
                        new AccountListService.AccountRow(42, "Inactive Employee", "inactive@example.test", "inactive42", "HR", "People", "Inactive"),
                        new AccountListService.AccountRow(43, "Blocked Employee", "blocked@example.test", "blocked43", "HR", "People", "Blocked"))));
        MockHttpSession adminSession = login(account("admin", "System Admin", "Active"));
        mvc.perform(get("/admin/accounts").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"Deactivate account active41\"")))
                .andExpect(content().string(containsString("aria-label=\"Deactivate account blocked43\"")))
                .andExpect(content().string(not(containsString("/admin/accounts/42/deactivate"))))
                .andExpect(content().string(containsString("/admin/accounts/42/edit")))
                .andExpect(content().string(not(containsString("Delete account"))))
                .andExpect(content().string(not(containsString("Account Deletion"))));
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
                .andExpect(content().string(containsString("Passwords do not match.")))
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
        when(accountManagementService.createInternal(create)).thenReturn(42);
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
        verify(accountManagementService).createInternal(create);

        AccountManagementService.UpdateCommand update = new AccountManagementService.UpdateCommand(
                "Name mới", "updated@example.com", null, 2, 1, "Inactive");
        mvc.perform(post("/admin/accounts/42").session(adminSession)
                        .param(csrf.getParameterName(), csrf.getToken())
                        .param("fullName", "Name mới")
                        .param("email", "updated@example.com")
                        .param("roleId", "2")
                        .param("departmentId", "1")
                        .param("accountStatus", "Inactive")
                        .param("username", "tampered-username")
                        .param("password", "tampered-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"));
        verify(accountManagementService).updateInternal(42, update);
    }

    @Test
    void everyConfirmedRoleCanOpenDashboard() throws Exception {
        String[] roles = {"System Admin", "HR", "Hiring Manager", "Director", "Interviewer", "Candidate"};
        for (int index = 0; index < roles.length; index++) {
            MockHttpSession session = login(account("role" + index, roles[index], "Active"));
            mvc.perform(get("/dashboard").session(session)).andExpect(status().isOk())
                    .andExpect(content().string("Candidate".equals(roles[index])
                            ? not(containsString("href=\"/requisitions\""))
                            : containsString("href=\"/requisitions\"")));
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

    @Test
    void sharedUiKeepsServerRenderedRoutesCsrfAndAccountFields() throws Exception {
        prepareUiOptions();
        when(passwordResetService.isValid("visual-token")).thenReturn(true);
        String[][] authPages = {{"/login", "login"}, {"/register", "register"},
                {"/forgot-password", "forgot"}, {"/reset-password/visual-token", "reset"},
                {"/login?error", "login-error"}, {"/login?logout", "login-logout"},
                {"/forgot-password?sent", "forgot-sent"}, {"/reset-password/expired-visual", "reset-expired"}};
        for (String[] entry : authPages) {
            MvcResult result = mvc.perform(get(entry[0])).andExpect(status().isOk())
                    .andExpect(content().string(containsString("/css/design-tokens.css")))
                    .andExpect(content().string(containsString("/js/interface.js")))
                    .andReturn();
            exportUi(entry[1], result);
        }
        MockHttpSession session = login(account("visual-admin", "System Admin", "Active"));
        when(dashboardService.forUsername("visual-admin")).thenReturn(new DashboardView(
                "System Admin", "Alex Nguyen", "System-wide account administration.",
                List.of(), List.of(new DashboardView.Breakdown("API and Integration Health", List.of(new com.group2.rms.repository.DashboardMetricsRepository.StatusCount("Available", 1)))), List.of(),
                List.of(new DashboardView.Unavailable("AI Configuration", "Configuration keys are awaiting confirmation.")),
                List.of(new DashboardView.Shortcut("API Monitoring", "/admin/api-monitoring")),
                List.of(new DashboardView.AccountSummary("Internal Accounts", "/admin/accounts", 8, 6, 1, 1),
                        new DashboardView.AccountSummary("Candidate Accounts", "/admin/candidate-accounts", 3, 1, 1, 1))));
        PageImpl<AccountListService.AccountRow> visualAccounts = new PageImpl<>(
                List.of(new AccountListService.AccountRow(42, "Nguyễn Minh Anh", "minhanh@example.test", "minhanh", "Interviewer", "Engineering", "Active"),
                        new AccountListService.AccountRow(43, "Taylor Morgan", "taylor@example.test", "taylor", "HR", "People", "Inactive"),
                        new AccountListService.AccountRow(44, "Jordan Lee", "jordan@example.test", "jordan", "Hiring Manager", "Engineering", "Blocked")),
                PageRequest.of(0, 3), 8);
        when(accountListService.findAccounts("", null, null, "", "name", 0)).thenReturn(visualAccounts);
        when(accountListService.findAccounts("", null, 1, "", "newest", 0)).thenReturn(visualAccounts);
        when(accountManagementService.findForEdit(42)).thenReturn(new AccountManagementService.AccountForEdit(
                "minhanh", "Nguyễn Minh Anh", "minhanh@example.test", null, 5, 1, "Active"));
        when(monitoringService.rows()).thenReturn(List.of(new ApiMonitoringService.MonitorRow(
                "internal", "Internal", "Application and SQL Server", "GET /admin/api-monitoring/internal/health",
                "OPERATIONAL", "Operational", 12L, 0, 0, 200, LocalDateTime.of(2026, 10, 1, 9, 0), 1, true),
                new ApiMonitoringService.MonitorRow("ai", "External Integration", "AI CV Screening", "No endpoint configured",
                        "UNCONFIGURED", "Not Configured", null, null, null, null, null, 0, false)));
        String[][] adminPages = {{"/dashboard", "dashboard"}, {"/admin/accounts", "accounts"},
                {"/admin/accounts/new", "create"}, {"/admin/accounts/42/edit", "edit"}, {"/admin/api-monitoring", "monitoring"},
                {"/admin/accounts?departmentId=1&sort=newest", "accounts-filtered"}};
        for (String[] entry : adminPages) {
            MvcResult result = mvc.perform(get(entry[0]).session(session)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("name=\"_csrf\"")))
                    .andReturn();
            String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
            org.junit.jupiter.api.Assertions.assertEquals(1,
                    java.util.regex.Pattern.compile("action=\"/logout\"").matcher(html).results().count());
            org.junit.jupiter.api.Assertions.assertEquals(1,
                    java.util.regex.Pattern.compile("<main id=\"main\"").matcher(html).results().count());
            exportUi(entry[1], result);
        }
        mvc.perform(get("/admin/accounts/42/edit").session(session))
                .andExpect(content().string(containsString("Nguyễn Minh Anh")))
                .andExpect(content().string(containsString("readonly")))
                .andExpect(content().string(not(containsString("name=\"password\""))))
                .andExpect(content().string(not(containsString("name=\"username\""))));
        mvc.perform(get("/admin/accounts/new").session(session))
                .andExpect(content().string(containsString("name=\"confirmPassword\"")))
                .andExpect(content().string(not(containsString("name=\"accountStatus\""))));
        verifyNoInteractions(registrationService, passwordResetEmailSender);
    }

    @Test
    void invalidFormsAssociateErrorsAndPreserveNonSecretInputWithoutServiceWrites() throws Exception {
        prepareUiOptions();
        MvcResult registration = mvc.perform(get("/register")).andReturn();
        CsrfToken token = (CsrfToken) registration.getRequest().getAttribute(CsrfToken.class.getName());
        MockHttpSession guest = (MockHttpSession) registration.getRequest().getSession(false);
        MvcResult invalidRegister = mvc.perform(post("/register").session(guest)
                        .param(token.getParameterName(), token.getToken()).param("fullName", "Alex Nguyen")
                        .param("username", "alex").param("email", "invalid-email")
                        .param("password", "validPassword12").param("confirmPassword", "differentPassword"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-describedby=\"email-error\"")))
                .andExpect(content().string(containsString("data-validation-summary")))
                .andExpect(content().string(containsString("value=\"invalid-email\"")))
                .andExpect(content().string(not(containsString("value=\"validPassword12\""))))
                .andReturn();
        exportUi("register-invalid", invalidRegister);
        MockHttpSession admin = login(account("visual-admin", "System Admin", "Active"));
        MvcResult create = mvc.perform(get("/admin/accounts/new").session(admin)).andReturn();
        CsrfToken csrf = (CsrfToken) create.getRequest().getAttribute(CsrfToken.class.getName());
        MvcResult invalidCreate = mvc.perform(post("/admin/accounts").session(admin)
                        .param(csrf.getParameterName(), csrf.getToken()).param("fullName", "Taylor Morgan")
                        .param("username", "taylor").param("email", "invalid-email").param("roleId", "2")
                        .param("departmentId", "1").param("password", "validPassword12")
                        .param("confirmPassword", "differentPassword"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-describedby=\"email-error\"")))
                .andExpect(content().string(containsString("aria-describedby=\"confirm-password-error\"")))
                .andExpect(content().string(not(containsString("value=\"validPassword12\""))))
                .andReturn();
        exportUi("create-invalid", invalidCreate);
        when(accountManagementService.findForEdit(42)).thenReturn(new AccountManagementService.AccountForEdit(
                "minhanh", "Nguyễn Minh Anh", "minhanh@example.test", null, 5, 1, "Active"));
        MvcResult invalidEdit = mvc.perform(post("/admin/accounts/42").session(admin)
                        .param(csrf.getParameterName(), csrf.getToken()).param("fullName", "Nguyễn Minh Anh")
                        .param("email", "invalid-email").param("roleId", "6").param("accountStatus", "Active"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("data-validation-summary")))
                .andExpect(content().string(not(containsString("name=\"password\"")))).andReturn();
        exportUi("edit-invalid", invalidEdit);
        verifyNoInteractions(registrationService, passwordResetEmailSender);
        org.mockito.Mockito.verify(accountManagementService, org.mockito.Mockito.atLeastOnce()).findForEdit(42);
        org.mockito.Mockito.verify(accountManagementService, org.mockito.Mockito.never()).createInternal(any());
        org.mockito.Mockito.verify(accountManagementService, org.mockito.Mockito.never()).updateInternal(org.mockito.ArgumentMatchers.anyInt(), any());
    }

    private void prepareUiOptions() {
        when(accountListService.findRoles()).thenReturn(List.of(
                Role.builder().roleId(1).roleName("System Admin").build(), Role.builder().roleId(2).roleName("HR").build(),
                Role.builder().roleId(3).roleName("Hiring Manager").build(), Role.builder().roleId(4).roleName("Director").build(),
                Role.builder().roleId(5).roleName("Interviewer").build()));
        when(accountListService.findDepartments()).thenReturn(List.of(
                Department.builder().departmentId(1).departmentName("Engineering").build(),
                Department.builder().departmentId(2).departmentName("People").build()));
    }

    private void exportUi(String page, MvcResult result) throws Exception {
        if (!Boolean.getBoolean("ui.preview")) return;
        Path directory = Path.of("target", "auth-admin-preview", "preview", page);
        Files.createDirectories(directory);
        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8)
                .replaceAll("(name=\"_csrf\" value=\")[^\"]+", "$1visual-csrf-fixture");
        Files.writeString(directory.resolve("index.html"), html, StandardCharsets.UTF_8);
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
                List.of(new DashboardView.Metric("Metric thử nghiệm", 0, "Dữ liệu kiểm thử")),
                List.of(),
                "Director".equals(roleName)
                        ? List.of(new ApprovalActivity("Job Requisition", "Vị trí kiểm thử", "Approved",
                                LocalDateTime.of(2026, 9, 28, 9, 0)))
                        : List.of(),
                List.of(),
                "System Admin".equals(roleName)
                        ? List.of(new DashboardView.Shortcut("Account Management", "/admin/accounts"),
                                new DashboardView.Shortcut("AI Configuration (Unavailable)", null))
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
