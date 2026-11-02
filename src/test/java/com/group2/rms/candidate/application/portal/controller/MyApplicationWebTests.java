package com.group2.rms.candidate.application.portal.controller;

import com.group2.rms.candidate.application.portal.dto.MyApplicationListResponse;
import com.group2.rms.candidate.application.portal.dto.MyApplicationResponse;
import com.group2.rms.candidate.application.portal.dto.MyApplicationStep;
import com.group2.rms.candidate.application.portal.exception.ApplicationWithdrawalException;
import com.group2.rms.candidate.application.portal.service.MyApplicationService;
import com.group2.rms.candidate.infra.cv.ApplicationCv;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.core.web.TimelineItem;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
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
 * "Đơn của tôi" của ứng viên (danh sách, chi tiết, CV, rút đơn) qua Spring MVC + Spring Security thật; service là mock.
 */
@WebMvcTest(MyApplicationController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class MyApplicationWebTests {

    private static final LocalDateTime SUBMITTED = LocalDateTime.of(2026, 10, 5, 9, 0);

    @Autowired MockMvc mvc;
    @MockitoBean MyApplicationService myApplications;
    @MockitoBean UserRepository users;

    // ---- Quyền ----

    @Test
    void guestIsSentToLogin() throws Exception {
        for (String url : List.of("/portal/applications", "/portal/applications/42", "/portal/applications/42/cv")) {
            mvc.perform(get(url)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        }
        verifyNoInteractions(myApplications);
    }

    @Test
    void internalStaffIsForbidden() throws Exception {
        for (String[] staff : List.of(new String[] {"hr_lan", "HR"}, new String[] {"hm_tuan", "Hiring Manager"})) {
            for (String url : List.of("/portal/applications", "/portal/applications/42", "/portal/applications/42/cv")) {
                mvc.perform(get(url).with(as(staff[0], staff[1]))).andExpect(status().isForbidden());
            }
            mvc.perform(post("/portal/applications/42/withdraw").with(as(staff[0], staff[1])).with(csrf()))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(myApplications);
    }

    // ---- Danh sách ----

    @Test
    void listShowsTabsWithCountsAndMarksTheCurrentTab() throws Exception {
        when(myApplications.list("active")).thenReturn(new MyApplicationListResponse(
                List.of(card(42).build()), "active", 4, 3, 1, List.of()));

        mvc.perform(get("/portal/applications").param("tab", "active").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/my-applications"))
                .andExpect(model().attribute("activeMenu", "applications"))
                .andExpect(model().attribute("tab", "active"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(content().string(containsString("href=\"/portal/applications\"")))
                .andExpect(content().string(containsString("href=\"/portal/applications?tab=ended\"")))
                .andExpect(content().string(matchesRegex("(?s).*href=\"/portal/applications\\?tab=active\"\\s+aria-current=\"page\">Đang xử lý\\s+<span class=\"myapp-tab__count\">3</span>.*")))
                .andExpect(content().string(containsString("<span class=\"myapp-tab__count\">4</span>")))
                .andExpect(content().string(not(containsString("?step="))))
                .andExpect(content().string(not(containsString("style=\""))));
    }

    @Test
    void tabTypedByHandIsLeftToTheServiceToNormalise() throws Exception {
        when(myApplications.list("abc")).thenReturn(page(List.of(), "all"));

        mvc.perform(get("/portal/applications").param("tab", "abc").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tab", "all"));
        verify(myApplications).list("abc");
    }

    @Test
    void cardShowsIdentityStepperStatusAndLinksButNoWithdraw() throws Exception {
        when(myApplications.list(null)).thenReturn(page(List.of(card(42)
                .status("Phỏng vấn 10:00 24/11", "Trực tuyến. Xem chi tiết ở Lịch phỏng vấn.", "warning")
                .action("Xem lịch phỏng vấn", "/portal/interviews")
                .steps("done", "current", "upcoming", "upcoming")
                .canWithdraw(true)
                .build()), "all"));

        mvc.perform(get("/portal/applications").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk())
                // Dòng 1: định danh + Xem lại CV
                .andExpect(content().string(containsString("href=\"/portal/applications/42\">Junior Java Developer</a>")))
                .andExpect(content().string(containsString("Engineering")))
                .andExpect(content().string(not(containsString("#CAN-0042"))))
                .andExpect(content().string(containsString("05/10/2026")))
                .andExpect(content().string(containsString("href=\"/portal/applications/42/cv\"")))
                .andExpect(content().string(containsString("data-cv-open")))
                .andExpect(content().string(containsString("id=\"myapp-cv-dialog\"")))
                // Dòng 2: thanh 4 bước + chữ trạng thái + nút
                .andExpect(content().string(stringContainsInOrder("data-state=\"done\"", "Nộp CV",
                        "data-state=\"current\"", "Phỏng vấn", "data-state=\"upcoming\"", "Thư mời",
                        "data-state=\"upcoming\"", "Nhận việc")))
                .andExpect(content().string(containsString("Phỏng vấn 10:00 24/11")))
                .andExpect(content().string(containsString("Trực tuyến. Xem chi tiết ở Lịch phỏng vấn.")))
                .andExpect(content().string(containsString("href=\"/portal/interviews\"")))
                .andExpect(content().string(containsString("btn--primary")))
                .andExpect(content().string(containsString("Xem chi tiết đơn")))
                // Rút đơn chỉ ở trang chi tiết
                .andExpect(content().string(not(containsString("/portal/applications/42/withdraw"))))
                .andExpect(content().string(not(containsString("myapp-withdraw-dialog"))))
                .andExpect(content().string(not(containsString("data-urgent"))))
                .andExpect(content().string(not(containsString("myapp-urgent-banner"))));
    }

    @Test
    void urgentCardStandsOutAndBannerPointsToTheNearestTask() throws Exception {
        MyApplicationResponse offer = card(25).title("Senior Java Backend")
                .status("Có thư mời", "Vui lòng trả lời trước 29/10.", "warning")
                .action("Trả lời thư mời", "/portal/offers/3")
                .urgent(LocalDateTime.of(2026, 10, 29, 23, 59), "Còn 2 ngày để trả lời thư mời").build();
        MyApplicationResponse interview = card(26).title("QA Engineer")
                .urgent(LocalDateTime.of(2026, 10, 30, 9, 0), "Phỏng vấn sau 40 giờ").build();
        when(myApplications.list(null)).thenReturn(new MyApplicationListResponse(
                List.of(offer, interview), "all", 2, 2, 0, List.of(offer, interview)));

        mvc.perform(get("/portal/applications").with(as("huy.vu", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-urgent=\"true\"")))
                .andExpect(content().string(containsString("Còn 2 ngày để trả lời thư mời")))
                .andExpect(content().string(containsString("myapp-urgent-banner")))
                .andExpect(content().string(containsString("Bạn có 2 việc cần chú ý")))
                .andExpect(content().string(stringContainsInOrder("Gần nhất:", "Senior Java Backend",
                        "còn 2 ngày để trả lời thư mời", "href=\"/portal/offers/3\"", "Trả lời thư mời")));
    }

    /** Đơn dừng (dunglt 27/10): Nộp CV vẫn ✓, cả thanh xám, chữ trạng thái thành badge, không còn [X]. */
    @Test
    void stoppedCardHasGreyBarWithTicksAndBadgeInsteadOfCross() throws Exception {
        when(myApplications.list(null)).thenReturn(page(List.of(card(44)
                .status("Đã tự rút hồ sơ", "Bạn đã rút hồ sơ ngày 25/10.", "neutral")
                .steps("done", "upcoming", "upcoming", "upcoming").stopped().build()), "all"));

        mvc.perform(get("/portal/applications").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-state=\"stopped\"")))
                .andExpect(content().string(containsString("data-stopped=\"true\"")))
                .andExpect(content().string(stringContainsInOrder("data-state=\"done\"", "<polyline points=\"20 6 9 17 4 12\"/>",
                        "Nộp CV")))
                .andExpect(content().string(containsString("class=\"badge myapp-badge-stopped\">Đã tự rút hồ sơ</span>")))
                .andExpect(content().string(not(containsString("data-state=\"closed\""))))
                .andExpect(content().string(not(containsString("<line x1=\"18\" y1=\"6\""))));
    }

    @Test
    void cardWithoutCvHasNoCvLink() throws Exception {
        when(myApplications.list(null)).thenReturn(page(List.of(card(42).noCv().build()), "all"));

        mvc.perform(get("/portal/applications").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/portal/applications/42/cv"))))
                .andExpect(content().string(not(containsString("Xem lại CV"))));
    }

    @Test
    void candidateWithoutApplicationsIsPointedToOpenJobs() throws Exception {
        when(myApplications.list(null)).thenReturn(page(List.of(), "all"));

        mvc.perform(get("/portal/applications").with(as("new.candidate", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bạn chưa ứng tuyển vị trí nào")))
                .andExpect(content().string(containsString("Xem vị trí đang tuyển")))
                .andExpect(content().string(containsString("href=\"/jobs\"")))
                .andExpect(content().string(not(containsString("class=\"myapp-tabs\""))));
    }

    @Test
    void emptyTabSaysSoWithItsOwnSentence() throws Exception {
        when(myApplications.list("active")).thenReturn(new MyApplicationListResponse(List.of(), "active", 1, 0, 1, List.of()));
        when(myApplications.list("ended")).thenReturn(new MyApplicationListResponse(List.of(), "ended", 1, 1, 0, List.of()));

        mvc.perform(get("/portal/applications").param("tab", "active").with(as("an.vo", "Candidate")))
                .andExpect(content().string(containsString("Không có đơn đang xử lý.")))
                .andExpect(content().string(not(containsString("Bạn chưa ứng tuyển vị trí nào"))));
        mvc.perform(get("/portal/applications").param("tab", "ended").with(as("an.vo", "Candidate")))
                .andExpect(content().string(containsString("Chưa có đơn nào kết thúc.")));
    }

    // ---- Chi tiết ----

    /** Lưới 2 cột (dunglt 27/10): trái Tiến độ → Thông báo → Hành trình; phải Thông tin & Tài liệu → Rút đơn. */
    @Test
    void detailShowsProgressNoticeJourneyThenInfoAndWithdraw() throws Exception {
        when(myApplications.detail(42)).thenReturn(card(42).canWithdraw(true)
                .journey(new TimelineItem(SUBMITTED, "Đã nộp hồ sơ", null, null, "brand")).build());

        mvc.perform(get("/portal/applications/42").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/my-application-detail"))
                .andExpect(model().attribute("activeMenu", "applications"))
                .andExpect(content().string(stringContainsInOrder("href=\"/portal/applications\"", "Đơn của tôi",
                        "Junior Java Developer", "Engineering",
                        "class=\"myapp-detail__main\"", "Tiến độ", "myapp-stepper",
                        "Thông báo", "Đã nộp CV", "Hành trình hồ sơ", "Đã nộp hồ sơ", "05/10/2026 09:00",
                        "class=\"myapp-detail__side\"", "Thông tin &amp; Tài liệu", "Mã đơn", "#CAN-0042",
                        "Ngày nộp", "05/10/2026", "href=\"/portal/applications/42/cv\"",
                        "href=\"/jobs/7\"", "Xem tin tuyển dụng", "href=\"mailto:careers@moc.com\"",
                        "action=\"/portal/applications/42/withdraw\"", "</aside>")))
                .andExpect(content().string(containsString("aria-label=\"Rút đơn vị trí Junior Java Developer\"")))
                .andExpect(content().string(containsString("data-posting-title=\"Junior Java Developer\"")))
                .andExpect(content().string(containsString("id=\"myapp-withdraw-dialog\"")))
                .andExpect(content().string(containsString("id=\"myapp-cv-dialog\"")))
                .andExpect(content().string(not(containsString("style=\""))));
    }

    @Test
    void detailOfApplicationStoppedOnClosedPostingHasGreyBarBadgeAndNoWithdrawNorPostingLink() throws Exception {
        when(myApplications.detail(42)).thenReturn(card(42).postingClosed()
                .status("Vị trí đã ngừng tuyển", null, "neutral").steps("done", "upcoming", "upcoming", "upcoming")
                .stopped().build());

        mvc.perform(get("/portal/applications/42").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-stopped=\"true\"")))
                .andExpect(content().string(containsString("class=\"badge myapp-badge-stopped\">Vị trí đã ngừng tuyển")))
                .andExpect(content().string(containsString("Tin tuyển dụng đã đóng.")))
                .andExpect(content().string(not(containsString("href=\"/jobs/7\""))))
                .andExpect(content().string(not(containsString("/withdraw"))))
                .andExpect(content().string(not(containsString("myapp-withdraw-dialog"))));
    }

    @Test
    void detailOfSomeoneElsesApplicationIsNotFound() throws Exception {
        when(myApplications.detail(25)).thenThrow(new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển."));

        mvc.perform(get("/portal/applications/25").with(as("an.vo", "Candidate")))
                .andExpect(status().isNotFound());
    }

    // ---- CV ----

    @Test
    void uploadedCvIsServedInlineAndMayBeFramedBySameOrigin() throws Exception {
        when(myApplications.cv(42)).thenReturn(ApplicationCv.file(new ByteArrayResource("%PDF-1.7".getBytes())));

        mvc.perform(get("/portal/applications/42/cv").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("inline")))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
                .andExpect(content().string("%PDF-1.7"));
    }

    @Test
    void sampleCvRedirectsToTheStaticFile() throws Exception {
        when(myApplications.cv(42)).thenReturn(ApplicationCv.redirect("/samples/cv/sample_cv_java.pdf"));

        mvc.perform(get("/portal/applications/42/cv").with(as("an.vo", "Candidate")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/samples/cv/sample_cv_java.pdf"));
    }

    @Test
    void cvOfSomeoneElsesApplicationIsNotFound() throws Exception {
        when(myApplications.cv(25)).thenThrow(new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển."));

        mvc.perform(get("/portal/applications/25/cv").with(as("an.vo", "Candidate")))
                .andExpect(status().isNotFound());
    }

    // ---- Rút đơn ----

    @Test
    void withdrawRedirectsToTheListWithSuccessMessage() throws Exception {
        when(myApplications.withdraw(301)).thenReturn("Junior Java Developer");

        mvc.perform(post("/portal/applications/301/withdraw").with(as("an.vo", "Candidate")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/applications"))
                .andExpect(flash().attribute("successMessage",
                        "Đã rút hồ sơ ứng tuyển vị trí Junior Java Developer."));
    }

    @Test
    void refusedWithdrawGoesBackToTheDetailWithTheReason() throws Exception {
        when(myApplications.withdraw(120))
                .thenThrow(new ApplicationWithdrawalException("Hồ sơ đã qua vòng sơ tuyển nên không thể rút."));

        mvc.perform(post("/portal/applications/120/withdraw").with(as("an.vo", "Candidate")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/applications/120"))
                .andExpect(flash().attribute("errorMessage", "Hồ sơ đã qua vòng sơ tuyển nên không thể rút."));
    }

    @Test
    void refusalMessageIsShownOnTheDetailPage() throws Exception {
        when(myApplications.detail(120)).thenReturn(card(120).build());

        mvc.perform(get("/portal/applications/120").with(as("an.vo", "Candidate"))
                        .flashAttr("errorMessage", "Hồ sơ vừa được cập nhật. Vui lòng tải lại trang."))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("alert alert--danger")))
                .andExpect(content().string(containsString("Hồ sơ vừa được cập nhật. Vui lòng tải lại trang.")));
    }

    @Test
    void withdrawWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/portal/applications/301/withdraw").with(as("an.vo", "Candidate")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(myApplications);
    }

    // ---- Dựng dữ liệu ----

    private static MyApplicationListResponse page(List<MyApplicationResponse> cards, String tab) {
        int ended = (int) cards.stream().filter(MyApplicationResponse::ended).count();
        return new MyApplicationListResponse(cards, tab, cards.size(), cards.size() - ended, ended, List.of());
    }

    private static Card card(int id) {
        return new Card(id);
    }

    /** Thẻ mặc định: đơn đang ở bước Nộp CV, có CV, tin còn hiện, không gấp, không rút được. */
    private static final class Card {
        private final int id;
        private String title = "Junior Java Developer";
        private List<MyApplicationStep> steps = stepList("current", "upcoming", "upcoming", "upcoming");
        private String label = "Đã nộp CV";
        private String note = "Hồ sơ của bạn đang được xem xét.";
        private String tone = "neutral";
        private String actionLabel;
        private String actionHref;
        private LocalDateTime urgentUntil;
        private String urgentLabel;
        private boolean ended;
        private boolean stopped;
        private boolean canWithdraw;
        private boolean hasCv = true;
        private boolean postingVisible = true;
        private List<TimelineItem> journey = List.of();

        Card(int id) {
            this.id = id;
        }

        Card title(String value) { title = value; return this; }
        Card status(String l, String n, String t) { label = l; note = n; tone = t; return this; }
        Card action(String l, String href) { actionLabel = l; actionHref = href; return this; }
        Card steps(String... states) { steps = stepList(states); return this; }
        Card urgent(LocalDateTime until, String l) { urgentUntil = until; urgentLabel = l; return this; }
        Card ended() { ended = true; return this; }
        Card stopped() { ended = true; stopped = true; return this; }
        Card canWithdraw(boolean value) { canWithdraw = value; return this; }
        Card noCv() { hasCv = false; return this; }
        Card postingClosed() { postingVisible = false; return this; }
        Card journey(TimelineItem... items) { journey = List.of(items); return this; }

        MyApplicationResponse build() {
            return new MyApplicationResponse(id, 7, title, "Engineering", String.format("#CAN-%04d", id), SUBMITTED,
                    SUBMITTED, steps, label, note, tone, actionLabel, actionHref, urgentUntil, urgentLabel, ended,
                    stopped, ended ? 3 : urgentUntil != null ? 1 : 2, urgentUntil, canWithdraw, hasCv, postingVisible,
                    journey);
        }

        private static List<MyApplicationStep> stepList(String... states) {
            List<String> labels = List.of("Nộp CV", "Phỏng vấn", "Thư mời", "Nhận việc");
            return java.util.stream.IntStream.range(0, 4)
                    .mapToObj(i -> new MyApplicationStep(labels.get(i), states[i])).toList();
        }
    }

    private RequestPostProcessor as(String username, String roleName) {
        User account = User.builder().userId(1).username(username).fullName(username).accountStatus("Active")
                .role(Role.builder().roleName(roleName).build()).build();
        when(users.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(account));
        String authority = switch (roleName) {
            case "HR" -> "ROLE_HR";
            case "Hiring Manager" -> "ROLE_HIRING_MANAGER";
            default -> "ROLE_CANDIDATE";
        };
        return user(username).authorities(() -> authority);
    }
}
