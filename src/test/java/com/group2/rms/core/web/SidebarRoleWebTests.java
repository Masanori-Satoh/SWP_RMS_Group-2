package com.group2.rms.core.web;

import com.group2.rms.candidate.controller.ApplicationPipelineController;
import com.group2.rms.candidate.controller.MyApplicationController;
import com.group2.rms.candidate.dto.ApplicationListResponse;
import com.group2.rms.candidate.dto.ApplicationSearch;
import com.group2.rms.candidate.dto.MyApplicationListResponse;
import com.group2.rms.candidate.service.ApplicationPipelineService;
import com.group2.rms.candidate.service.ApplicationReviewService;
import com.group2.rms.candidate.service.ApplicationStage;
import com.group2.rms.candidate.service.MyApplicationService;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.interview.controller.CandidateInterviewController;
import com.group2.rms.interview.service.InterviewSchedulingService;
import com.group2.rms.notification.NotificationController;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.offer.controller.CandidateOfferController;
import com.group2.rms.offer.service.CandidateOfferService;
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
import org.springframework.stereotype.Controller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sidebar chọn menu theo vai trò của người đang đăng nhập ({@code topbarUser}), không theo biến riêng của controller.
 * Dựng trang thật (controller + Spring Security thật, service là mock): 3 trang /portal/* không đưa role vào model,
 * trước đây rơi vào nhánh "mặc định nhân sự" và vẽ sidebar HR (link /applications, /offers → 403).
 */
@WebMvcTest({MyApplicationController.class, CandidateOfferController.class, CandidateInterviewController.class,
        NotificationController.class, ApplicationPipelineController.class, SidebarRoleWebTests.ProbeController.class})
@Import({SecurityConfig.class, DatabaseUserDetailsService.class, SidebarRoleWebTests.ProbeController.class})
class SidebarRoleWebTests {

    @Autowired MockMvc mvc;
    @MockitoBean UserRepository users;
    @MockitoBean MyApplicationService myApplications;
    @MockitoBean CandidateOfferService candidateOffers;
    @MockitoBean InterviewSchedulingService interviewScheduling;
    @MockitoBean CurrentUserService currentUser;
    @MockitoBean NotificationService notifications;
    @MockitoBean ApplicationPipelineService pipeline;
    @MockitoBean ApplicationReviewService reviewService;
    @MockitoBean com.group2.rms.candidate.service.TalentPoolService talentPool;

    @Test
    void candidatePagesDrawCandidateSidebar() throws Exception {
        User candidate = account("an.vo", "Candidate");
        when(currentUser.requireUser()).thenReturn(candidate);
        when(notifications.getNotificationsForUser(any(), anyInt(), anyInt())).thenReturn(Page.empty());
        when(myApplications.list(any())).thenReturn(new MyApplicationListResponse(
                List.of(), Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L), null, 0));

        for (String page : List.of("/portal/applications", "/portal/offers", "/portal/interviews", "/notifications")) {
            String sidebar = sidebar(page, as(candidate));
            assertTrue(sidebar.contains("href=\"/portal/offers\""), page + ": thiếu menu ứng viên");
            assertFalse(sidebar.contains("href=\"/applications\""), page + ": lộ menu HR /applications");
            assertFalse(sidebar.contains("href=\"/offers\""), page + ": lộ menu HR /offers");
        }
    }

    @Test
    void recruiterKeepsRecruiterSidebar() throws Exception {
        when(pipeline.list(any(), any())).thenReturn(new ApplicationListResponse(
                new PageImpl<>(List.of(), Pageable.ofSize(20), 0), zeroCounts(), List.of(),
                new ApplicationSearch(null, null, null, "score", null).normalized(), null, true, false, "HR"));

        String sidebar = sidebar("/applications", as(account("hr_lan", "HR")));
        assertTrue(sidebar.contains("href=\"/applications\""));
        assertTrue(sidebar.contains("href=\"/internal/job-postings\""), "mục chỉ HR thấy");
        assertFalse(sidebar.contains("href=\"/portal/offers\""));
    }

    /** {@code fragments/sidebar :: sidebar} gán cứng 'HR': vai trò thật của người đăng nhập phải thắng. */
    @Test
    void legacySidebarFragmentFollowsSignedInRole() throws Exception {
        String sidebar = sidebar("/test/legacy-sidebar-probe", as(account("director", "Director")));
        assertTrue(sidebar.contains("href=\"/requisitions\""));
        assertTrue(sidebar.contains("href=\"/offers\""));
        assertFalse(sidebar.contains("href=\"/internal/job-postings\""), "Director không có mục chỉ HR thấy");
    }

    private String sidebar(String page, RequestPostProcessor viewer) throws Exception {
        String html = mvc.perform(get(page).with(viewer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int start = html.indexOf("<nav class=\"sidebar-menu-list\"");
        assertTrue(start >= 0, page + ": không có sidebar");
        return html.substring(start, html.indexOf("</nav>", start));
    }

    /** Tài khoản đang hoạt động, để {@code AccountSessionGuardFilter} và {@code TopbarUserAdvice} tìm thấy. */
    private User account(String username, String roleName) {
        User account = User.builder().userId(1).username(username).fullName(username).accountStatus("Active")
                .role(Role.builder().roleName(roleName).build()).build();
        when(users.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(account));
        return account;
    }

    private static RequestPostProcessor as(User account) {
        String authority = "ROLE_" + account.getRole().getRoleName().toUpperCase();
        return user(account.getUsername()).authorities(() -> authority);
    }

    private static Map<ApplicationStage, Long> zeroCounts() {
        Map<ApplicationStage, Long> counts = new EnumMap<>(ApplicationStage.class);
        for (ApplicationStage stage : ApplicationStage.values()) {
            counts.put(stage, 0L);
        }
        return counts;
    }

    @Controller
    static class ProbeController {
        @GetMapping("/test/legacy-sidebar-probe")
        String probe() {
            return "test/legacy-sidebar-probe";
        }
    }
}
