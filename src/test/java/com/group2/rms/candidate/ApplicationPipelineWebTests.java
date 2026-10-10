package com.group2.rms.candidate;

import com.group2.rms.candidate.controller.ApplicationPipelineController;
import com.group2.rms.candidate.dto.ApplicationListResponse;
import com.group2.rms.candidate.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.dto.ApplicationSearch;
import com.group2.rms.candidate.dto.JobPostingOption;
import com.group2.rms.candidate.service.ApplicationPipelineService;
import com.group2.rms.core.config.SecurityConfig;
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
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Trang hồ sơ ứng tuyển qua Spring MVC + Spring Security thật; service là mock.
 */
@WebMvcTest(ApplicationPipelineController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class ApplicationPipelineWebTests {

    private static final LocalDateTime SUBMITTED = LocalDateTime.of(2026, 10, 10, 14, 59);

    @Autowired MockMvc mvc;
    @MockitoBean ApplicationPipelineService pipeline;
    @MockitoBean UserRepository users;

    // ------------------------------------------------------------------ quyền truy cập

    @Test
    void guestIsSentToLogin() throws Exception {
        mvc.perform(get("/applications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        verifyNoInteractions(pipeline);
    }

    @Test
    void candidateAndInterviewerAreForbidden() throws Exception {
        mvc.perform(get("/applications").with(as("phong.nguyen", "Candidate"))).andExpect(status().isForbidden());
        mvc.perform(get("/applications").with(as("iv_minh", "Interviewer"))).andExpect(status().isForbidden());
        verifyNoInteractions(pipeline);
    }

    // ------------------------------------------------------------------ danh sách

    @Test
    void recruiterSeesCandidateListWithRowActions() throws Exception {
        when(pipeline.list(any(), any())).thenReturn(listOf(false, "HR",
                row(298, "AI_Screened", "79.34", false, false),
                row(120, "HM_Passed", "88.10", true, true),
                row(293, "Applied", null, false, false)));

        mvc.perform(get("/applications").with(as("hr_lan", "HR")))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/applications"))
                .andExpect(model().attribute("activeMenu", "applications"))
                .andExpect(model().attribute("viewerRole", "HR"))
                .andExpect(content().string(containsString("Danh sách ứng viên")))
                .andExpect(content().string(not(containsString("Hồ sơ được chuyển"))))
                .andExpect(content().string(containsString("href=\"/applications/298\"")))
                .andExpect(content().string(containsString("href=\"/applications/298/cv\"")))
                .andExpect(content().string(containsString(">79<")))
                .andExpect(content().string(containsString("Chờ AI chấm")))
                .andExpect(content().string(containsString("badge--warning")))
                .andExpect(content().string(containsString("href=\"/interviews/new?applicationId=120\"")))
                .andExpect(content().string(containsString("href=\"/offers/create?applicationId=120\"")))
                .andExpect(content().string(not(containsString("applicationId=298"))))
                .andExpect(content().string(containsString("class=\"more-menu\"")));
    }

    @Test
    void hiringManagerSeesForwardedTitleAndEmptyState() throws Exception {
        when(pipeline.list(any(), any())).thenReturn(listOf(true, "Hiring Manager"));

        mvc.perform(get("/applications").with(as("hm_khanh", "Hiring Manager")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("viewerRole", "Hiring Manager"))
                .andExpect(content().string(containsString("Hồ sơ được chuyển")))
                .andExpect(content().string(containsString("khi HR chuyển cho phòng ban của bạn")))
                .andExpect(content().string(not(containsString("<table"))));
    }

    @Test
    void directorAndAdminCanOpenTheList() throws Exception {
        when(pipeline.list(any(), any())).thenReturn(listOf(false, "Director"));

        mvc.perform(get("/applications").with(as("director", "Director"))).andExpect(status().isOk());
        mvc.perform(get("/applications").with(as("admin", "System Admin"))).andExpect(status().isOk());
    }

    @Test
    void filtersAndPageAreHandedToServiceAndKeptInForm() throws Exception {
        when(pipeline.list(any(), any())).thenReturn(listOf(false, "HR"));

        mvc.perform(get("/applications").with(as("hr_lan", "HR"))
                        .param("jobPostingId", "6").param("status", "Rejected")
                        .param("keyword", "an.vo").param("sort", "newest").param("page", "2"))
                .andExpect(status().isOk());

        // "sort" cũng bị Spring Data đọc vào Pageable; service bỏ qua sort của Pageable nên chỉ so trang + cỡ trang.
        verify(pipeline).list(eq(new ApplicationSearch(6, "Rejected", "an.vo", "newest")),
                argThat(p -> p.getPageNumber() == 2 && p.getPageSize() == 20));
    }

    // ------------------------------------------------------------------ dữ liệu mẫu

    /** Đăng nhập giả + tài khoản đang hoạt động, để {@code AccountSessionGuardFilter} cho qua. */
    private RequestPostProcessor as(String username, String roleName) {
        User account = User.builder().userId(1).username(username).fullName(username).accountStatus("Active")
                .role(Role.builder().roleName(roleName).build()).build();
        when(users.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(account));
        String authority = switch (roleName) {
            case "System Admin" -> "ROLE_SYSTEM_ADMIN";
            case "Hiring Manager" -> "ROLE_HIRING_MANAGER";
            default -> "ROLE_" + roleName.toUpperCase();
        };
        return user(username).authorities(() -> authority);
    }

    private static ApplicationListResponse listOf(boolean forwardedView, String viewerRole,
                                                  ApplicationPipelineResponse... rows) {
        Page<ApplicationPipelineResponse> page = new PageImpl<>(List.of(rows), Pageable.ofSize(20), rows.length);
        return new ApplicationListResponse(page, List.of(new JobPostingOption(6, "Digital Marketing Executive")),
                new ApplicationSearch(null, null, null, "score").normalized(), forwardedView, viewerRole);
    }

    private static ApplicationPipelineResponse row(int id, String status, String score, boolean schedule,
                                                   boolean offer) {
        return new ApplicationPipelineResponse(id, "An Võ", "an.vo@example.com", "Digital Marketing Executive",
                SUBMITTED, status, score == null ? null : new BigDecimal(score)).withActions(schedule, offer);
    }
}
