package com.group2.rms;

import com.group2.rms.auth.controller.AuthController;
import com.group2.rms.career.controller.CareerPortalController;
import com.group2.rms.career.dto.DepartmentFilterResponse;
import com.group2.rms.career.dto.PublicJobDetailResponse;
import com.group2.rms.career.dto.PublicJobListResponse;
import com.group2.rms.career.dto.ViewerProfileResponse;
import com.group2.rms.career.service.CareerPortalService;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ CareerPortalController.class, AuthController.class })
@Import({ SecurityConfig.class, DatabaseUserDetailsService.class })
class CareerFlowTests {
        private static final LocalDateTime FUTURE_DEADLINE = LocalDateTime.now().plusDays(30).withNano(0);
        private static final LocalDateTime POSTED = LocalDateTime.of(2026, 9, 25, 8, 0);

        @Autowired
        MockMvc mvc;
        @Autowired
        PasswordEncoder encoder;
        @MockitoBean
        CareerPortalService careers;
        @MockitoBean
        UserRepository users;

        // ------------------------------------------------------------------ job board

        @Test
        void publicJobBoardRendersPublishedJobsForGuests() throws Exception {
                setupJobs();
                MvcResult home = mvc.perform(get("/")).andExpect(status().isOk())
                                .andExpect(view().name("candidate/job-board"))
                                .andExpect(content().string(containsString("Mộc Careers")))
                                .andExpect(content().string(containsString("href=\"/jobs/41\"")))
                                .andExpect(content().string(containsString("href=\"/jobs/42\"")))
                                .andExpect(content().string(containsString("Senior Java Engineer")))
                                .andExpect(content().string(containsString("VND 30–45 million")))
                                .andExpect(content().string(containsString("Engineering")))
                                .andExpect(content().string(containsString("Đăng nhập")))
                                .andExpect(content().string(not(containsString("Đăng xuất"))))
                                .andReturn();
                snapshot("index.html", home);

                mvc.perform(get("/jobs")).andExpect(status().isOk())
                                .andExpect(view().name("candidate/job-board"))
                                .andExpect(content().string(containsString("href=\"/jobs/41\"")));
                mvc.perform(get("/fonts/career-2.woff2")).andExpect(status().isOk());
                mvc.perform(get("/rms/").contextPath("/rms")).andExpect(status().isOk())
                                .andExpect(content().string(containsString("href=\"/rms/jobs/41\"")));
        }

        @Test
        void jobBoardTrimsFiltersAndTreatsBlankValuesAsNull() throws Exception {
                setupJobs();
                mvc.perform(get("/jobs").param("keyword", "  java  ").param("departmentId", "1")
                                .param("employmentType", "   "))
                                .andExpect(status().isOk())
                                .andExpect(model().attribute("selectedKeyword", "java"))
                                .andExpect(model().attribute("selectedDept", 1))
                                .andExpect(model().attribute("selectedType", nullValue()));
                verify(careers).getPublishedJobs(eq("java"), eq(1), isNull(), any(Pageable.class));

                mvc.perform(get("/jobs").param("keyword", "   ").param("employmentType", " Full-time "))
                                .andExpect(status().isOk());
                verify(careers).getPublishedJobs(isNull(), isNull(), eq("Full-time"), any(Pageable.class));
        }

        @Test
        void emptyJobBoardShowsRecoveryState() throws Exception {
                when(careers.getPublishedJobs(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());
                when(careers.getSmartSortedDepartments()).thenReturn(List.of());
                mvc.perform(get("/")).andExpect(status().isOk())
                                .andExpect(content().string(containsString("Không tìm thấy kết quả")))
                                .andExpect(content().string(containsString("Xóa bộ lọc")));
        }

        // ------------------------------------------------------------------ job detail

        @Test
        void jobDetailRendersOpenJobWithApplyLink() throws Exception {
                setupJobs();
                MvcResult detail = mvc.perform(get("/jobs/41")).andExpect(status().isOk())
                                .andExpect(view().name("candidate/job-detail"))
                                .andExpect(content().string(
                                                containsString("<title>Senior Java Engineer — Mộc Careers</title>")))
                                .andExpect(content().string(containsString("VND 30–45 million")))
                                .andExpect(content().string(containsString("Hà Nội")))
                                .andExpect(content().string(containsString("onclick=\"alert('Tính năng ứng tuyển trực tuyến đang được xây dựng")))
                                .andExpect(content().string(containsString("Ứng tuyển ngay")))
                                .andExpect(content().string(not(containsString("Ngừng nhận hồ sơ"))))
                                .andReturn();
                snapshot("jobs/41/index.html", detail);
                verify(careers).getPublishedJobDetail(41, null);
        }

        @Test
        void closedJobDetailHidesApplyLink() throws Exception {
                setupJobs();
                when(careers.getPublishedJobDetail(eq(43), any())).thenReturn(detail(43, "Closed Role",
                                LocalDateTime.now().minusDays(1), false, false));
                mvc.perform(get("/jobs/43")).andExpect(status().isOk())
                                .andExpect(content().string(containsString("Ngừng nhận hồ sơ")))
                                .andExpect(content().string(not(containsString("href=\"/jobs/43/apply\""))));
        }

        @Test
        void alreadyAppliedJobDetailShowsAppliedState() throws Exception {
                setupJobs();
                when(careers.getPublishedJobDetail(eq(44), any())).thenReturn(detail(44, "Applied Role",
                                FUTURE_DEADLINE, true, true));
                mvc.perform(get("/jobs/44")).andExpect(status().isOk())
                                .andExpect(content().string(containsString("Đã ứng tuyển")))
                                .andExpect(content().string(not(containsString("href=\"/jobs/44/apply\""))));
        }

        @Test
        void unavailableJobReturnsNotFoundPage() throws Exception {
                when(careers.getPublishedJobDetail(eq(999), any()))
                                .thenThrow(new ResourceNotFoundException("Tin tuyển dụng này không còn khả dụng."));
                mvc.perform(get("/jobs/999")).andExpect(status().isNotFound())
                                .andExpect(view().name("error/404"))
                                .andExpect(content().string(containsString("404 - Không Tìm Thấy")))
                                .andExpect(content().string(containsString("Tin tuyển dụng này không còn khả dụng.")));
        }

        // ------------------------------------------------------------------ apply flow

        @Test
        void guestApplyRedirectsToLoginAndReturnsToSameApplyUrl() throws Exception {
                setupJobs();
                account("candidate-test", "Candidate", "Active");
                MvcResult protectedPage = mvc.perform(get("/jobs/41/apply"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("http://localhost/login")).andReturn();
                MockHttpSession session = (MockHttpSession) protectedPage.getRequest().getSession(false);

                session = login(session, "candidate-test", "http://localhost/jobs/41/apply");

                // Online submission is pending Iteration 2: the controller validates and then
                // reports it via the error page.
                mvc.perform(get("/jobs/41/apply").session(session))
                                .andExpect(view().name("error/500"))
                                .andExpect(model().attribute("errorCode", "ITERATION_2_PENDING"))
                                .andExpect(content().string(containsString("Iteration 2")));
                verify(careers).validateJobForApplication(41);
                verify(careers).validateCandidateApplicationProfile("candidate-test");
        }

        @Test
        void applyForUnavailableJobReturnsNotFound() throws Exception {
                account("candidate-test", "Candidate", "Active");
                MockHttpSession session = login(null, "candidate-test", "/dashboard");
                doThrow(new ResourceNotFoundException(
                                "Tin tuyển dụng này không còn khả dụng hoặc đã ngừng nhận hồ sơ."))
                                .when(careers).validateJobForApplication(999);
                mvc.perform(get("/jobs/999/apply").session(session)).andExpect(status().isNotFound())
                                .andExpect(view().name("error/404"));
                verify(careers, never()).validateCandidateApplicationProfile(anyString());
        }

        @Test
        void internalAccountIsDeniedApplyPage() throws Exception {
                setupJobs();
                account("hr-test", "HR", "Active");
                MockHttpSession hr = login(null, "hr-test", "/dashboard");
                mvc.perform(get("/jobs/41/apply").session(hr)).andExpect(status().isForbidden());
                verify(careers, never()).validateJobForApplication(anyInt());
                verify(careers, never()).validateCandidateApplicationProfile(anyString());
        }

        @Test
        void disabledSessionIsRevokedBeforeApplyPage() throws Exception {
                account("inactive-test", "Candidate", "Active");
                MockHttpSession session = login(null, "inactive-test", "/dashboard");
                User user = users.findByUsernameIgnoreCase("inactive-test").orElseThrow();
                user.setAccountStatus("Inactive");
                mvc.perform(get("/jobs/41/apply").session(session)).andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/login?session-expired"));
                verify(careers, never()).validateJobForApplication(anyInt());
                verify(careers, never()).validateCandidateApplicationProfile(anyString());
        }

        // ------------------------------------------------------------------
        // authenticated header

        @Test
        void authenticatedViewerSeesProfileAndLogoutRequiresCsrf() throws Exception {
                setupJobs();
                account("candidate-test", "Candidate", "Active");
                MockHttpSession session = login(null, "candidate-test", "/dashboard");

                MvcResult home = mvc.perform(get("/").session(session)).andExpect(status().isOk())
                                .andExpect(content().string(containsString("Taylor Nguyen")))
                                .andExpect(content().string(containsString("taylor@example.test")))
                                .andExpect(content().string(containsString("Đăng xuất")))
                                .andExpect(content().string(containsString("name=\"_csrf\"")))
                                .andExpect(content().string(not(containsString("Đăng nhập</a>"))))
                                .andReturn();
                snapshot("authenticated.html", home);
                verify(careers, atLeastOnce()).getViewerProfile("candidate-test");

                mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
                CsrfToken csrf = (CsrfToken) home.getRequest().getAttribute(CsrfToken.class.getName());
                mvc.perform(post("/logout").session(session).param(csrf.getParameterName(), csrf.getToken()))
                                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?logout"));
        }

        @Test
        void guestDoesNotTriggerViewerLookup() throws Exception {
                setupJobs();
                mvc.perform(get("/")).andExpect(status().isOk());
                verify(careers, never()).getViewerProfile(anyString());
        }

        // ------------------------------------------------------------------ helpers

        private void setupJobs() {
                PublicJobListResponse job = new PublicJobListResponse(41, "Senior Java Engineer", "Engineering",
                                "Hà Nội",
                                "Full-time", "VND 30–45 million", FUTURE_DEADLINE, POSTED);
                PublicJobListResponse second = new PublicJobListResponse(42, "Product Designer", "Design",
                                "Ho Chi Minh City",
                                "Full-time", "VND 22–30 million", FUTURE_DEADLINE, POSTED);
                when(careers.getPublishedJobs(any(), any(), any(), any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(job, second), PageRequest.of(0, 9), 2));
                when(careers.getSmartSortedDepartments()).thenReturn(List.of(
                                new DepartmentFilterResponse(1, "Engineering"),
                                new DepartmentFilterResponse(2, "Design"),
                                new DepartmentFilterResponse(3, "Operations")));
                when(careers.getPublishedJobDetail(eq(41), any()))
                                .thenReturn(detail(41, "Senior Java Engineer", FUTURE_DEADLINE, false, true));
        }

        private PublicJobDetailResponse detail(int id, String title, LocalDateTime deadline,
                        boolean hasApplied, boolean accepting) {
                return new PublicJobDetailResponse(id, title, "Engineering", "Hà Nội", "Full-time",
                                "VND 30–45 million", deadline, POSTED,
                                "Build clear, dependable workflow software.",
                                List.of("Build clear, dependable workflow software."),
                                "Experience with Java and Spring Boot.",
                                List.of("Experience with Java and Spring Boot."),
                                "Role-specific development opportunities.",
                                List.of("Role-specific development opportunities."),
                                hasApplied, accepting);
        }

    private void account(String username, String role, String status) {
        User user = User.builder().userId(10).username(username).fullName("Taylor Nguyen")
                .email("taylor@example.test").accountStatus(status).role(Role.builder().roleName(role).build())
                .passwordHash(encoder.encode("test-password")).build();
        when(users.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(user));
        when(careers.getViewerProfile(username))
                .thenReturn(Optional.of(new ViewerProfileResponse(user.getFullName(), user.getEmail())));
    }

        private MockHttpSession login(MockHttpSession session, String username, String expected) throws Exception {
                var request = get("/login");
                if (session != null)
                        request.session(session);
                MvcResult page = mvc.perform(request).andReturn();
                CsrfToken csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
                return (MockHttpSession) mvc
                                .perform(post("/login").session((MockHttpSession) page.getRequest().getSession(false))
                                                .param(csrf.getParameterName(), csrf.getToken())
                                                .param("username", username).param("password", "test-password"))
                                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(expected)).andReturn()
                                .getRequest().getSession(false);
        }

        /**
         * Opt-in visual fixtures only. Never exported as application data or served by
         * production.
         */
        private void snapshot(String filename, MvcResult result) throws Exception {
                if (!Boolean.getBoolean("career.preview"))
                        return;
                Path target = Path.of("target/career-preview").resolve(filename);
                Files.createDirectories(target.getParent());
                Files.writeString(target, result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                                StandardCharsets.UTF_8);
        }
}
