package com.group2.rms;

import com.group2.rms.auth.AuthController;
import com.group2.rms.auth.CareerController;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.service.CareerService;
import com.group2.rms.service.CareerService.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({CareerController.class, AuthController.class})
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class CareerFlowTests {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockitoBean CareerService careers;
    @MockitoBean UserRepository users;

    @Test
    void publicJobsAreRenderedByThymeleafAndRetainRealMetadata() throws Exception {
        setupJobs();
        MvcResult home = mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"en\"")))
                .andExpect(content().string(containsString("href=\"/jobs/41\"")))
                .andExpect(content().string(containsString("Sign In")))
                .andExpect(content().string(not(containsString("const jobs"))))
                .andReturn();
        snapshot("index.html", home);
        MvcResult detail = mvc.perform(get("/jobs/41")).andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Senior Java Engineer | Mộc Careers</title>")))
                .andExpect(content().string(containsString("Online submission is not available yet")))
                .andExpect(content().string(containsString("VND 30–45 million")))
                .andExpect(content().string(containsString("2026-09-25")))
                .andExpect(content().string(containsString("/jobs/41/apply")))
                .andExpect(content().string(containsString("&lt;script&gt;unsafe&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>unsafe</script>"))))
                .andReturn();
        snapshot("jobs/41/index.html", detail);
        mvc.perform(get("/fonts/career-2.woff2")).andExpect(status().isOk());
        mvc.perform(get("/rms/").contextPath("/rms")).andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/rms/jobs/41\"")));
    }

    @Test
    void guestLoginReturnsToSameApplyUrlAndPrefillsOnlyOwnCandidate() throws Exception {
        setupJobs();
        account("candidate-test", "Candidate", "Active");
        when(careers.candidateDetails("candidate-test")).thenReturn(Optional.of(new CandidateDetails(
                "Taylor Nguyen", "taylor@example.test", "0900000000", "https://linkedin.com/in/test", null, "Hanoi")));
        MvcResult protectedPage = mvc.perform(get("/jobs/41/apply"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("http://localhost/login")).andReturn();
        MockHttpSession session = (MockHttpSession) protectedPage.getRequest().getSession(false);
        session = login(session, "candidate-test", "http://localhost/jobs/41/apply");
        MvcResult apply = mvc.perform(get("/jobs/41/apply").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Apply for Senior Java Engineer | Mộc Careers</title>")))
                .andExpect(content().string(containsString("value=\"Taylor Nguyen\"")))
                .andExpect(content().string(containsString("value=\"taylor@example.test\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("Online submission is not available yet")))
                .andExpect(content().string(containsString("Log Out")))
                .andExpect(content().string(not(containsString("Sign In</a>"))))
                .andReturn();
        snapshot("jobs/41/apply/index.html", apply);
        snapshot("authenticated.html", mvc.perform(get("/").session(session)).andReturn());
        verify(careers).candidateDetails("candidate-test");
        mvc.perform(post("/jobs/41/apply").session(session)).andExpect(status().isForbidden());
        CsrfToken csrf = (CsrfToken) apply.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/jobs/41/apply").session(session).param(csrf.getParameterName(), csrf.getToken()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(session).param(csrf.getParameterName(), csrf.getToken()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void internalAccountDeniedAndMissingProfileIsNotAutoCreated() throws Exception {
        setupJobs();
        account("hr-test", "HR", "Active");
        MockHttpSession hr = login(null, "hr-test", "/dashboard");
        mvc.perform(get("/jobs/41/apply").session(hr)).andExpect(status().isForbidden());
        verify(careers, never()).candidateDetails("hr-test");
        account("missing-test", "Candidate", "Active");
        MockHttpSession candidate = login(null, "missing-test", "/dashboard");
        mvc.perform(get("/jobs/41/apply").session(candidate)).andExpect(status().isConflict())
                .andExpect(content().string(containsString("Your candidate profile is missing")));
    }

    @Test
    void emptyAndUnavailableJobsHaveRecoveryAndNoPrivateData() throws Exception {
        when(careers.openJobs()).thenReturn(List.of());
        when(careers.departments()).thenReturn(List.of());
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("There are no open roles right now")));
        mvc.perform(get("/jobs/999")).andExpect(status().isNotFound())
                .andExpect(content().string(containsString("<title>This role is no longer available | Mộc Careers</title>")))
                .andExpect(content().string(containsString("View Open Positions")));
    }

    @Test
    void disabledSessionCannotAccessPrefilledForm() throws Exception {
        account("inactive-test", "Candidate", "Active");
        MockHttpSession session = login(null, "inactive-test", "/dashboard");
        User user = users.findByUsernameIgnoreCase("inactive-test").orElseThrow();
        user.setAccountStatus("Inactive");
        mvc.perform(get("/jobs/41/apply").session(session)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?session-expired"));
        verify(careers, never()).candidateDetails(anyString());
    }

    private void setupJobs() {
        PublicJob job = new PublicJob(41, "Senior Java Engineer", 1, "Engineering", "Full-time", "Hà Nội",
                "VND 30–45 million", LocalDateTime.of(2026, 9, 25, 8, 0), null,
                List.of("Build clear, dependable workflow software.", "<script>unsafe</script>"),
                List.of("Experience with Java and Spring Boot.", "Explain technical decisions clearly."),
                List.of("Role-specific development opportunities."));
        PublicJob second = new PublicJob(42, "Product Designer", 2, "Design", "Full-time", "Ho Chi Minh City",
                "VND 22–30 million", job.postingDate(), null, List.of("Design useful tools."), List.of("Portfolio of product work."), List.of());
        when(careers.openJobs()).thenReturn(List.of(job, second));
        when(careers.departments()).thenReturn(List.of(new DepartmentOption(1, "Engineering"), new DepartmentOption(2, "Design"), new DepartmentOption(3, "Operations")));
        when(careers.openJob(41)).thenReturn(Optional.of(job));
    }

    private void account(String username, String role, String status) {
        User user = User.builder().userId(10).username(username).fullName("Taylor Nguyen")
                .email("taylor@example.test").accountStatus(status).role(Role.builder().roleName(role).build())
                .passwordHash(encoder.encode("test-password")).build();
        when(users.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(user));
        when(careers.viewer(username)).thenReturn(Optional.of(new Viewer(user.getFullName(), role)));
    }

    private MockHttpSession login(MockHttpSession session, String username, String expected) throws Exception {
        var request = get("/login");
        if (session != null) request.session(session);
        MvcResult page = mvc.perform(request).andReturn();
        CsrfToken csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        return (MockHttpSession) mvc.perform(post("/login").session((MockHttpSession) page.getRequest().getSession(false))
                .param(csrf.getParameterName(), csrf.getToken()).param("username", username).param("password", "test-password"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(expected)).andReturn().getRequest().getSession(false);
    }

    /** Opt-in visual fixtures only. Never exported as application data or served by production. */
    private void snapshot(String filename, MvcResult result) throws Exception {
        if (!Boolean.getBoolean("career.preview")) return;
        Path target = Path.of("target/career-preview").resolve(filename);
        Files.createDirectories(target.getParent());
        Files.writeString(target, result.getResponse().getContentAsString(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
