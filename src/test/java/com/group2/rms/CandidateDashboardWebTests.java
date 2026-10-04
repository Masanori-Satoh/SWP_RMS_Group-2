package com.group2.rms;

import com.group2.rms.auth.controller.AuthController;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.dashboard.DashboardController;
import com.group2.rms.dashboard.DashboardMetricsRepository.StatusCount;
import com.group2.rms.dashboard.DashboardResponse;
import com.group2.rms.dashboard.DashboardResponse.*;
import com.group2.rms.dashboard.DashboardService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = { DashboardController.class, AuthController.class })
@Import({ SecurityConfig.class, DatabaseUserDetailsService.class })
class CandidateDashboardWebTests {
    @Autowired private MockMvc mvc;
    @Autowired private PasswordEncoder encoder;
    @MockitoBean private UserRepository users;
    @MockitoBean private DashboardService dashboard;

    @Test
    void candidatePageRendersVietnameseSharedShellAndEscapedOfficialDetails() throws Exception {
        var session = login("Candidate", populated());
        var result = mvc.perform(get("/dashboard").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"vi\"")))
                .andExpect(content().string(containsString("Đơn ứng tuyển của tôi")))
                .andExpect(content().string(containsString("/css/design-tokens.css")))
                .andExpect(content().string(containsString("Lương thử việc / tháng")))
                .andExpect(content().string(containsString("17.000.000 ₫")))
                .andExpect(content().string(containsString("&lt;script&gt;unsafe()&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>unsafe()"))))
                .andExpect(content().string(not(containsString("Candidate.UserId"))))
                .andExpect(content().string(not(containsString("href=\"/requisitions\""))))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andReturn();
        fixture("populated", result);
    }

    @Test
    void emptyCandidatePageProvidesRealCareersLinkAndNoFakeCounts() throws Exception {
        var result = mvc.perform(get("/dashboard").session(login("Candidate", empty())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bạn chưa có đơn ứng tuyển")))
                .andExpect(content().string(containsString("Bạn chưa nhận được thư mời")))
                .andExpect(content().string(containsString("href=\"/jobs\"")))
                .andExpect(content().string(containsString("Chưa khả dụng"))).andReturn();
        fixture("empty", result);
    }

    @Test
    void candidateLogoutKeepsCsrfAndRevokesSession() throws Exception {
        var session = login("Candidate", empty());
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        var page = mvc.perform(get("/dashboard").session(session)).andReturn();
        var csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/logout").session(session).param(csrf.getParameterName(), csrf.getToken()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?logout"));
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
    }

    @Test
    void internalDashboardKeepsExistingEnglishLayout() throws Exception {
        var view = new DashboardResponse("HR", "HR fixture", "Existing scope", List.of(new Metric("Open Job Postings", 2, "Published")),
                List.of(), List.of(), List.of(), List.of());
        mvc.perform(get("/dashboard").session(login("HR", view))).andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"en\"")))
                .andExpect(content().string(containsString("Open Job Postings")))
                .andExpect(content().string(not(containsString("candidate-dashboard.js"))))
                .andExpect(content().string(containsString("id=\"workspace-logout-dialog\"")));
    }

    @Test
    void guestCannotOpenCandidateDashboard() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "System Admin", "HR", "Hiring Manager", "Director", "Interviewer", "Candidate" })
    void restoredWorkspaceKeepsMainMenusAndSharedLogoutForEveryRole(String role) throws Exception {
        var response = "Candidate".equals(role) ? empty() : new DashboardResponse(role, "Menu fixture", "Role scope",
                List.of(), List.of(), List.of(), List.of(), List.of());
        var result = mvc.perform(get("/dashboard").session(login(role, response))).andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/interviews\"")))
                .andExpect(content().string(containsString("href=\"/profile\"")))
                .andExpect(content().string(containsString("id=\"workspace-logout-form\"")))
                .andExpect(content().string(containsString("id=\"workspace-logout-dialog\"")))
                .andExpect(content().string(containsString("action=\"/logout\"")))
                .andExpect(content().string(containsString("name=\"_csrf\""))).andReturn();
        var html = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(List.of("System Admin", "HR", "Director").contains(role),
                html.contains("href=\"/offers\""));
        org.junit.jupiter.api.Assertions.assertEquals("System Admin".equals(role),
                html.contains("href=\"/admin/departments\""));
        org.junit.jupiter.api.Assertions.assertEquals("Candidate".equals(role),
                html.contains("href=\"/dashboard#applications\""));
    }

    private MockHttpSession login(String role, DashboardResponse view) throws Exception {
        String username = "candidate-ui-fixture-" + role;
        var account = User.builder().userId(51).username(username).fullName(view.fullName())
                .role(Role.builder().roleName(role).build()).passwordHash(encoder.encode("ui-test-password"))
                .accountStatus("Active").build();
        when(users.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(account));
        when(dashboard.forUsername(username)).thenReturn(view);
        var page = mvc.perform(get("/login")).andReturn();
        var csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        var session = (MockHttpSession) page.getRequest().getSession();
        return (MockHttpSession) mvc.perform(post("/login").session(session)
                .param(csrf.getParameterName(), csrf.getToken()).param("username", username)
                .param("password", "ui-test-password")).andExpect(status().is3xxRedirection())
                .andReturn().getRequest().getSession();
    }

    private DashboardResponse empty() {
        return view(List.of(), List.of(), List.of(), 0);
    }

    private DashboardResponse populated() {
        return view(List.of(new ApplicationItem("Kỹ sư phần mềm", LocalDate.of(2026, 10, 1), null, "Đã nộp hồ sơ"),
                        new ApplicationItem("Điều phối dự án", LocalDate.of(2026, 9, 29), null, "Đang phỏng vấn")),
                List.of(new InterviewItem("Điều phối dự án", LocalDateTime.of(2026, 11, 1, 10, 0), LocalDateTime.of(2026, 11, 1, 11, 0),
                        "Trực tuyến", "https://meet.google.com/test-room", null)),
                List.of(new OfferItem("Kỹ sư phần mềm", new BigDecimal("20000000"), LocalDate.of(2026, 11, 15), "Chờ phản hồi",
                        new BigDecimal("17000000"), "Hà Nội", "Bảo hiểm sức khỏe\n<script>unsafe()</script>")), 2);
    }

    private DashboardResponse view(List<ApplicationItem> applications, List<InterviewItem> interviews, List<OfferItem> offers, long total) {
        return new DashboardResponse("Candidate", "Nguyễn Minh An", "Theo dõi hồ sơ, lịch phỏng vấn và thư mời dành cho bạn.",
                List.of(new Metric("Đơn ứng tuyển", total, "Tổng số hồ sơ bạn đã nộp"),
                        new Metric("Phỏng vấn sắp tới", interviews.size(), "Lịch đã được hẹn hoặc đổi lịch"),
                        new Metric("Thư mời chờ phản hồi", offers.size(), "Thư mời chính thức đã gửi cho bạn")),
                List.of(new Breakdown("Trạng thái đơn ứng tuyển", applications.isEmpty() ? List.of() : List.of(new StatusCount("Đã nộp hồ sơ", 1), new StatusCount("Đang phỏng vấn", 1))),
                        new Breakdown("Trạng thái thư mời", offers.isEmpty() ? List.of() : List.of(new StatusCount("Chờ phản hồi", 1)))),
                List.of(), List.of(), List.of(), List.of(), new CandidatePanel(applications, interviews, offers,
                        new CandidateProfile("Nguyễn Minh An", "candidate-ui@example.test", null)));
    }

    private void fixture(String name, MvcResult result) throws Exception {
        Path directory = Path.of("target", "candidate-preview", name);
        Files.createDirectories(directory);
        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8)
                .replaceAll("(name=\"_csrf\" value=\")[^\"]+", "$1visual-fixture-only");
        Files.writeString(directory.resolve("index.html"), html, StandardCharsets.UTF_8);
    }
}
