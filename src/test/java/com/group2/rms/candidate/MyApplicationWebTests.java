package com.group2.rms.candidate;

import com.group2.rms.candidate.controller.MyApplicationController;
import com.group2.rms.candidate.dto.MyApplicationResponse;
import com.group2.rms.candidate.exception.ApplicationWithdrawalException;
import com.group2.rms.candidate.service.MyApplicationService;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Trang "Hồ sơ đã ứng tuyển" của ứng viên qua Spring MVC + Spring Security thật; service là mock.
 */
@WebMvcTest(MyApplicationController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class MyApplicationWebTests {

    private static final LocalDateTime SUBMITTED = LocalDateTime.of(2026, 10, 5, 9, 0);

    @Autowired MockMvc mvc;
    @MockitoBean MyApplicationService myApplications;
    @MockitoBean UserRepository users;

    @Test
    void guestIsSentToLogin() throws Exception {
        mvc.perform(get("/portal/applications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        verifyNoInteractions(myApplications);
    }

    @Test
    void internalStaffIsForbidden() throws Exception {
        mvc.perform(get("/portal/applications").with(as("hr_lan", "HR"))).andExpect(status().isForbidden());
        mvc.perform(post("/portal/applications/1/withdraw").with(as("hr_lan", "HR")).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(myApplications);
    }

    @Test
    void candidateSeesOwnApplicationsWithWithdrawOnlyWhereAllowed() throws Exception {
        when(myApplications.list()).thenReturn(List.of(
                card(301, 7, "Junior Java Developer", "Đã nộp CV", "neutral", true),
                card(120, 3, "UI/UX Designer", "Chờ xét CV", "neutral", false),
                card(90, 2, "Kế toán tổng hợp", "Đã tự rút hồ sơ", "neutral", false)));

        mvc.perform(get("/portal/applications").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/my-applications"))
                .andExpect(model().attribute("activeMenu", "applications"))
                .andExpect(content().string(containsString("Hồ sơ đã ứng tuyển")))
                .andExpect(content().string(containsString("href=\"/jobs/7\"")))
                .andExpect(content().string(containsString("05/10/2026")))
                .andExpect(content().string(containsString("Đã tự rút hồ sơ")))
                .andExpect(content().string(containsString("action=\"/portal/applications/301/withdraw\"")))
                .andExpect(content().string(not(containsString("/portal/applications/120/withdraw"))))
                .andExpect(content().string(not(containsString("/portal/applications/90/withdraw"))))
                .andExpect(content().string(containsString("id=\"myapp-withdraw-dialog\"")))
                .andExpect(content().string(not(containsString("style=\""))));
    }

    @Test
    void candidateWithoutApplicationsSeesEmptyState() throws Exception {
        when(myApplications.list()).thenReturn(List.of());

        mvc.perform(get("/portal/applications").with(as("new.candidate", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bạn chưa ứng tuyển vị trí nào")))
                .andExpect(content().string(not(containsString("<table"))));
    }

    @Test
    void withdrawRedirectsWithSuccessMessage() throws Exception {
        when(myApplications.withdraw(301)).thenReturn("Junior Java Developer");

        mvc.perform(post("/portal/applications/301/withdraw").with(as("an.vo", "Candidate")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/applications"))
                .andExpect(flash().attribute("successMessage",
                        "Đã rút hồ sơ ứng tuyển vị trí Junior Java Developer."));
    }

    @Test
    void refusedWithdrawRedirectsWithErrorMessage() throws Exception {
        when(myApplications.withdraw(120))
                .thenThrow(new ApplicationWithdrawalException("Hồ sơ đã qua vòng sơ tuyển nên không thể rút."));

        mvc.perform(post("/portal/applications/120/withdraw").with(as("an.vo", "Candidate")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/applications"))
                .andExpect(flash().attribute("errorMessage", "Hồ sơ đã qua vòng sơ tuyển nên không thể rút."));
    }

    @Test
    void withdrawWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/portal/applications/301/withdraw").with(as("an.vo", "Candidate")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(myApplications);
    }

    private static MyApplicationResponse card(int id, int jobPostingId, String title, String statusLabel,
                                              String tone, boolean canWithdraw) {
        return new MyApplicationResponse(id, jobPostingId, title, SUBMITTED, List.of(), statusLabel, null, tone,
                null, null, canWithdraw, List.of());
    }

    private RequestPostProcessor as(String username, String roleName) {
        User account = User.builder().userId(1).username(username).fullName(username).accountStatus("Active")
                .role(Role.builder().roleName(roleName).build()).build();
        when(users.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(account));
        String authority = "HR".equals(roleName) ? "ROLE_HR" : "ROLE_CANDIDATE";
        return user(username).authorities(() -> authority);
    }
}
