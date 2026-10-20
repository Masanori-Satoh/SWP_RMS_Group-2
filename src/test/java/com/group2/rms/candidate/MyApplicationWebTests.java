package com.group2.rms.candidate;

import com.group2.rms.candidate.controller.MyApplicationController;
import com.group2.rms.candidate.dto.MyApplicationListResponse;
import com.group2.rms.candidate.dto.MyApplicationResponse;
import com.group2.rms.candidate.exception.ApplicationWithdrawalException;
import com.group2.rms.candidate.service.MyApplicationService;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.core.web.TimelineItem;
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
import java.util.Map;
import java.util.TreeMap;
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
        when(myApplications.list(null)).thenReturn(page(null,
                card(301, 7, "Junior Java Developer", "Đã nộp CV", "neutral", true),
                card(120, 3, "UI/UX Designer", "Chờ xét CV", "neutral", false),
                card(90, 2, "Kế toán tổng hợp", "Đã tự rút hồ sơ", "neutral", false)));

        mvc.perform(get("/portal/applications").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/my-applications"))
                .andExpect(model().attribute("activeMenu", "applications"))
                .andExpect(content().string(containsString("Đơn ứng tuyển của tôi")))
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
    void cardShowsGuideStepTagStatusNextActionAndJourney() throws Exception {
        List<TimelineItem> journey = List.of(
                new TimelineItem(SUBMITTED, "Đã nộp hồ sơ", null, null, "brand"),
                new TimelineItem(LocalDateTime.of(2026, 10, 12, 9, 0), "Nhận thư mời nhận việc", null, null, "warning"));
        when(myApplications.list(null)).thenReturn(page(null, new MyApplicationResponse(25, 4, "Senior Java Backend",
                SUBMITTED, SUBMITTED, 4, "Thư mời", "current", "Có thư mời", "Vui lòng trả lời trước 17/10.", "warning",
                "Trả lời thư mời", "/portal/offers/3", false, journey)));

        mvc.perform(get("/portal/applications").with(as("huy.vu", "Candidate")))
                .andExpect(status().isOk())
                // Dải hướng dẫn 4 bước đầu trang
                .andExpect(content().string(containsString("Bấm một bước để xem hồ sơ ở bước đó")))
                .andExpect(content().string(containsString("Bước 4")))
                .andExpect(content().string(containsString("Nhận thư mời và trả lời trước hạn.")))
                // Tag bước (cùng số với dải trên) + tình trạng
                .andExpect(content().string(containsString("data-state=\"current\" data-tone=\"warning\"")))
                .andExpect(content().string(containsString("Bước 4 · Thư mời")))
                .andExpect(content().string(containsString("(hồ sơ đang ở bước này)")))
                .andExpect(content().string(containsString("Có thư mời")))
                .andExpect(content().string(containsString("Vui lòng trả lời trước 17/10.")))
                // Việc tiếp theo: nút chính (icon + chữ) vì cần ứng viên trả lời
                .andExpect(content().string(containsString("href=\"/portal/offers/3\"")))
                .andExpect(content().string(containsString("btn--primary")))
                .andExpect(content().string(containsString("Trả lời thư mời")))
                .andExpect(content().string(not(containsString("/portal/applications/25/withdraw"))))
                // Hành trình trong <details>, không cần JS
                .andExpect(content().string(containsString("<summary class=\"myapp-journey__toggle\">")))
                .andExpect(content().string(containsString("Hành trình")))
                .andExpect(content().string(containsString("Nhận thư mời nhận việc")))
                .andExpect(content().string(containsString("12/10/2026 09:00")))
                .andExpect(content().string(not(containsString("style=\""))));
    }

    @Test
    void stoppedApplicationTagShowsWhereItStopped() throws Exception {
        when(myApplications.list(null)).thenReturn(page(null, new MyApplicationResponse(216, 9, "DevOps Engineer",
                SUBMITTED, SUBMITTED, 2, "Xét CV", "closed", "Đã tự rút hồ sơ", "Bạn đã rút hồ sơ ngày 20/10.", "neutral",
                null, null, false, List.of())));

        mvc.perform(get("/portal/applications").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-state=\"closed\" data-tone=\"neutral\"")))
                .andExpect(content().string(containsString("Bước 2 · Xét CV")))
                .andExpect(content().string(containsString("(hồ sơ dừng ở bước này)")))
                .andExpect(content().string(containsString("Bạn đã rút hồ sơ ngày 20/10.")))
                .andExpect(content().string(not(containsString("myapp-card__actions"))));
    }

    @Test
    void withdrawIsAnIconButtonWithAccessibleName() throws Exception {
        when(myApplications.list(null)).thenReturn(page(null,
                card(301, 7, "Junior Java Developer", "Đã nộp CV", "neutral", true)));

        mvc.perform(get("/portal/applications").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"Rút đơn vị trí Junior Java Developer\"")))
                .andExpect(content().string(containsString("title=\"Rút đơn\"")))
                .andExpect(content().string(containsString("data-posting-title=\"Junior Java Developer\"")));
    }

    @Test
    void guideStepsAreFiltersWithCountsAndSelectedStepCanBeCleared() throws Exception {
        when(myApplications.list(3)).thenReturn(new MyApplicationListResponse(
                List.of(card(179, 5, "QA Automation Engineer", "Chờ kết quả", "neutral", false)),
                Map.of(1, 0L, 2, 2L, 3, 1L, 4, 1L), 3, 4));

        mvc.perform(get("/portal/applications").param("step", "3").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedStep", 3))
                // Bước 1 dẫn tới /jobs, bước 2–4 là bộ lọc có số đếm
                .andExpect(content().string(containsString("Tìm vị trí")))
                .andExpect(content().string(containsString("href=\"/portal/applications?step=2\"")))
                .andExpect(content().string(containsString("2 hồ sơ")))
                .andExpect(content().string(containsString("href=\"/portal/applications?step=4\"")))
                // Bước đang lọc: aria-current, bấm lại thì bỏ lọc
                .andExpect(content().string(containsString("aria-current=\"true\"")))
                .andExpect(content().string(not(containsString("href=\"/portal/applications?step=3\""))))
                .andExpect(content().string(containsString("Hồ sơ ở Bước 3 · Phỏng vấn")))
                .andExpect(content().string(containsString("Xem tất cả (4)")))
                .andExpect(content().string(containsString("QA Automation Engineer")));
    }

    @Test
    void filterWithoutMatchesSaysSoAndOffersToClearIt() throws Exception {
        when(myApplications.list(4)).thenReturn(new MyApplicationListResponse(
                List.of(), Map.of(1, 0L, 2, 1L, 3, 0L, 4, 0L), 4, 1));

        mvc.perform(get("/portal/applications").param("step", "4").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Không có hồ sơ nào ở bước này.")))
                .andExpect(content().string(containsString("Xem tất cả (1)")))
                .andExpect(content().string(not(containsString("Bạn chưa ứng tuyển vị trí nào"))));
    }

    @Test
    void stepTypedByHandOutsideOneToFourShowsEverything() throws Exception {
        when(myApplications.list(null)).thenReturn(page(null));

        mvc.perform(get("/portal/applications").param("step", "abc").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk());
        mvc.perform(get("/portal/applications").param("step", "7").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk());
        verify(myApplications, times(2)).list(null);
    }

    @Test
    void cardShowsLastUpdateAndSortHint() throws Exception {
        when(myApplications.list(null)).thenReturn(page(null, new MyApplicationResponse(25, 4, "Senior Java Backend",
                SUBMITTED, LocalDateTime.of(2026, 10, 12, 9, 0), 4, "Thư mời", "current", "Có thư mời", null,
                "warning", "Trả lời thư mời", "/portal/offers/3", false, List.of())));

        mvc.perform(get("/portal/applications").with(as("huy.vu", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Mới cập nhật ở trên"))))
                .andExpect(content().string(containsString("Cập nhật")))
                .andExpect(content().string(containsString("12/10/2026")));
    }

    @Test
    void candidateWithoutApplicationsSeesEmptyState() throws Exception {
        when(myApplications.list(null)).thenReturn(page(null));

        mvc.perform(get("/portal/applications").with(as("new.candidate", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bạn chưa ứng tuyển vị trí nào")))
                .andExpect(content().string(not(containsString("class=\"myapp-list\""))));
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

    /** Trang xem tất cả gồm các thẻ cho sẵn; số đếm tính theo stepNumber của thẻ. */
    private static MyApplicationListResponse page(Integer selectedStep, MyApplicationResponse... cards) {
        Map<Integer, Long> counts = new TreeMap<>(Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L));
        for (MyApplicationResponse card : cards) {
            counts.merge(card.stepNumber(), 1L, Long::sum);
        }
        return new MyApplicationListResponse(List.of(cards), counts, selectedStep, cards.length);
    }

    private static MyApplicationResponse card(int id, int jobPostingId, String title, String statusLabel,
                                              String tone, boolean canWithdraw) {
        return new MyApplicationResponse(id, jobPostingId, title, SUBMITTED, SUBMITTED, 2, "Xét CV", "current", statusLabel,
                null, tone, null, null, canWithdraw, List.of());
    }

    private RequestPostProcessor as(String username, String roleName) {
        User account = User.builder().userId(1).username(username).fullName(username).accountStatus("Active")
                .role(Role.builder().roleName(roleName).build()).build();
        when(users.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(account));
        String authority = "HR".equals(roleName) ? "ROLE_HR" : "ROLE_CANDIDATE";
        return user(username).authorities(() -> authority);
    }
}
