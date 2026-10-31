package com.group2.rms.core.web;

import com.group2.rms.candidate.application.pipeline.controller.ApplicationPipelineController;
import com.group2.rms.candidate.application.portal.controller.MyApplicationController;
import com.group2.rms.candidate.application.pipeline.dto.ApplicationListResponse;
import com.group2.rms.candidate.application.pipeline.dto.ApplicationSearch;
import com.group2.rms.candidate.application.portal.dto.MyApplicationListResponse;
import com.group2.rms.candidate.application.pipeline.service.ApplicationPipelineService;
import com.group2.rms.candidate.application.pipeline.service.ApplicationReviewService;
import com.group2.rms.candidate.domain.rule.ApplicationStage;
import com.group2.rms.candidate.application.portal.service.MyApplicationService;
import com.group2.rms.candidate.talentpool.service.TalentPoolService;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.interview.controller.CandidateInterviewController;
import com.group2.rms.interview.service.InterviewSchedulingService;
import com.group2.rms.notification.controller.NotificationController;
import com.group2.rms.notification.service.NotificationService;
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
    @MockitoBean TalentPoolService talentPool;

    /** Ứng viên không có sidebar mà dùng topbar ứng viên (dunglt chốt 2026-10-20; menu topbar để trống), không lộ link của HR. */
    @Test
    void candidatePagesDrawCandidateTopbarInsteadOfSidebar() throws Exception {
        User candidate = account("an.vo", "Candidate");
        when(currentUser.requireUser()).thenReturn(candidate);
        when(notifications.getNotificationsForUser(any(), anyInt(), anyInt())).thenReturn(Page.empty());
        when(myApplications.list(any())).thenReturn(new MyApplicationListResponse(
                List.of(), "all", 0, 0, 0, List.of()));

        for (String page : List.of("/portal/applications", "/portal/offers", "/portal/interviews", "/notifications")) {
            String html = render(page, as(candidate));
            assertFalse(html.contains("<nav class=\"sidebar-menu-list\""), page + ": ứng viên không còn sidebar");
            String nav = cut(html, "<nav class=\"candidate-topbar__nav\"", page + ": không có menu ứng viên trên topbar");

            assertFalse(nav.contains("href=\"/applications\""), page + ": lộ menu HR /applications");
            assertFalse(nav.contains("href=\"/offers\""), page + ": lộ menu HR /offers");
            // ux.md mục 0: menu 2 mục (fragments/layout/candidate-nav)
            assertTrue(nav.contains("href=\"/jobs\""), page + ": thiếu mục Vị trí tuyển dụng");
            assertTrue(nav.contains("href=\"/portal/applications\""), page + ": thiếu mục Đơn của tôi");
            // "Đơn của tôi" sáng ở trang đơn, lịch phỏng vấn, thư mời; không sáng ở thông báo
            assertEquals(!"/notifications".equals(page), nav.contains("aria-current=\"page\">Đơn của tôi"), page);
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

    /** Footer Mộc (fragments/layout/public-footer) chỉ có ở trang ứng viên trong layout nội bộ, kèm lối tắt portal. */
    @Test
    void candidatePagesHavePublicFooterWithPortalLinksStaffPagesDoNot() throws Exception {
        User candidate = account("an.vo", "Candidate");
        when(currentUser.requireUser()).thenReturn(candidate);
        when(notifications.getNotificationsForUser(any(), anyInt(), anyInt())).thenReturn(Page.empty());
        when(myApplications.list(any())).thenReturn(new MyApplicationListResponse(
                List.of(), "all", 0, 0, 0, List.of()));
        for (String page : List.of("/portal/applications", "/portal/offers", "/portal/interviews", "/notifications")) {
            String html = render(page, as(candidate));
            assertTrue(html.contains("class=\"site-footer\""), page + ": thiếu footer");
            assertTrue(html.contains(">Đơn ứng tuyển của tôi</a></li>"), page + ": footer thiếu lối tắt tới đơn");
            assertFalse(html.contains("href=\"/portal/offers\">Thư mời"), page + ": footer không còn mục thư mời riêng");
            assertFalse(html.contains("Đăng ký ứng viên"), page + ": ứng viên đã đăng nhập không cần Đăng ký");
        }

        when(pipeline.list(any(), any())).thenReturn(new ApplicationListResponse(
                new PageImpl<>(List.of(), Pageable.ofSize(20), 0), zeroCounts(), List.of(),
                new ApplicationSearch(null, null, null, "score", null).normalized(), null, true, false, "HR"));
        assertFalse(render("/applications", as(account("hr_lan", "HR"))).contains("class=\"site-footer\""),
                "trang nhân sự không có footer công khai");
    }

    /** {@code fragments/sidebar :: sidebar} gán cứng 'HR': vai trò thật của người đăng nhập phải thắng. */
    @Test
    void legacySidebarFragmentFollowsSignedInRole() throws Exception {
        String sidebar = sidebar("/test/legacy-sidebar-probe", as(account("director", "Director")));
        assertTrue(sidebar.contains("href=\"/requisitions\""));
        assertTrue(sidebar.contains("href=\"/offers\""));
        assertTrue(sidebar.contains("href=\"/internal/job-postings\""), "Director được xem tin tuyển dụng");
        assertFalse(sidebar.contains("href=\"/talent-pool\""), "Director không có mục chỉ HR, HM thấy");
    }

    private String sidebar(String page, RequestPostProcessor viewer) throws Exception {
        return cut(render(page, viewer), "<nav class=\"sidebar-menu-list\"", page + ": không có sidebar");
    }

    private String render(String page, RequestPostProcessor viewer) throws Exception {
        return mvc.perform(get(page).with(viewer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Đoạn {@code <nav ...>...</nav>} bắt đầu bằng {@code navStart}. */
    private static String cut(String html, String navStart, String missing) {
        int start = html.indexOf(navStart);
        assertTrue(start >= 0, missing);
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
