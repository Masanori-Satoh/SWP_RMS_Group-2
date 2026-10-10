package com.group2.rms.candidate;

import com.group2.rms.candidate.controller.ApplicationPipelineController;
import com.group2.rms.candidate.dto.ApplicationDetailResponse;
import com.group2.rms.candidate.dto.ApplicationListResponse;
import com.group2.rms.candidate.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.dto.ApplicationReviewRequest;
import com.group2.rms.candidate.dto.ApplicationSearch;
import com.group2.rms.candidate.dto.JobPostingOption;
import com.group2.rms.candidate.exception.ApplicationReviewException;
import com.group2.rms.candidate.service.AiScreeningOutcome;
import com.group2.rms.candidate.service.ApplicationCv;
import com.group2.rms.candidate.service.ApplicationPipelineService;
import com.group2.rms.candidate.service.ApplicationReviewService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.access.AccessDeniedException;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @MockitoBean ApplicationReviewService reviewService;
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

    // ------------------------------------------------------------------ chi tiết

    @Test
    void recruiterSeesDetailWithLineageEmbeddedCvTimelineAndNextSteps() throws Exception {
        when(pipeline.detail(298)).thenReturn(detail(true, true, "https://linkedin.com/in/an-vo"));

        mvc.perform(get("/applications/298").with(as("hr_lan", "HR")))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/application-detail"))
                .andExpect(model().attribute("activeMenu", "applications"))
                .andExpect(model().attribute("viewerRole", "HR"))
                .andExpect(content().string(containsString("href=\"/requisitions/7\"")))
                .andExpect(content().string(containsString("Yêu cầu tuyển dụng REQ-007")))
                .andExpect(content().string(containsString("href=\"/internal/job-postings/12\"")))
                .andExpect(content().string(containsString("src=\"/applications/298/cv\"")))
                .andExpect(content().string(containsString(">79,34<")))
                .andExpect(content().string(containsString("HR: Đạt")))
                .andExpect(content().string(containsString("Hợp vị trí")))
                .andExpect(content().string(containsString("href=\"https://linkedin.com/in/an-vo\"")))
                .andExpect(content().string(containsString("href=\"/interviews/new?applicationId=298\"")))
                .andExpect(content().string(containsString("href=\"/offers/create?applicationId=298\"")));
    }

    @Test
    void hiringManagerGetsNoLinkToHrOnlyPagesAndUnsafeUrlsAreNotLinked() throws Exception {
        when(pipeline.detail(298)).thenReturn(detail(false, false, "javascript:alert(1)"));

        mvc.perform(get("/applications/298").with(as("hm_huong", "Hiring Manager")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/internal/job-postings/12"))))
                .andExpect(content().string(containsString("Tin Digital Marketing Executive")))
                .andExpect(content().string(not(containsString("href=\"javascript:"))))
                .andExpect(content().string(containsString("Bước tiếp theo")))
                .andExpect(content().string(not(containsString("Lên lịch phỏng vấn"))))
                .andExpect(content().string(not(containsString("app-review"))));
    }

    @Test
    void detailOutsideScopeIsForbiddenAndMissingIsNotFound() throws Exception {
        when(pipeline.detail(298)).thenThrow(new AccessDeniedException("no"));
        when(pipeline.detail(999)).thenThrow(new ResourceNotFoundException("missing"));

        mvc.perform(get("/applications/298").with(as("hm_trang", "Hiring Manager"))).andExpect(status().isForbidden());
        mvc.perform(get("/applications/999").with(as("hr_lan", "HR"))).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ CV + header nhúng khung

    @Test
    void storedCvIsServedInlineAndMayBeFramedBySameOrigin() throws Exception {
        when(pipeline.cv(298)).thenReturn(ApplicationCv.file(new ByteArrayResource("%PDF-1.7".getBytes())));

        mvc.perform(get("/applications/298/cv").with(as("hr_lan", "HR")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/pdf"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"CV-298.pdf\""))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
                .andExpect(content().string("%PDF-1.7"));
    }

    @Test
    void sampleCvRedirectsToStaticFileThatMayAlsoBeFramed() throws Exception {
        when(pipeline.cv(120)).thenReturn(ApplicationCv.redirect("/samples/cv/sample_cv_accountant.pdf"));

        mvc.perform(get("/applications/120/cv").with(as("hr_lan", "HR")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/samples/cv/sample_cv_accountant.pdf"));
        // /samples/** cần đăng nhập (anyRequest().authenticated()); khung CV gửi kèm phiên của người xem
        mvc.perform(get("/samples/cv/sample_cv_accountant.pdf").with(as("hr_lan", "HR")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    @Test
    void everyOtherPageStillRefusesToBeFramed() throws Exception {
        when(pipeline.list(any(), any())).thenReturn(listOf(false, "HR"));
        when(pipeline.detail(298)).thenReturn(detail(true, true, null));

        mvc.perform(get("/applications").with(as("hr_lan", "HR")))
                .andExpect(header().string("X-Frame-Options", "DENY"));
        mvc.perform(get("/applications/298").with(as("hr_lan", "HR")))
                .andExpect(header().string("X-Frame-Options", "DENY"));
        mvc.perform(get("/login")).andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void cvIsForbiddenToCandidates() throws Exception {
        mvc.perform(get("/applications/298/cv").with(as("phong.nguyen", "Candidate"))).andExpect(status().isForbidden());
        verifyNoInteractions(pipeline);
    }

    // ------------------------------------------------------------------ duyệt + chấm lại

    @Test
    void reviewerSeesReviewFormRescreenButtonAndCurrentStep() throws Exception {
        when(pipeline.detail(298)).thenReturn(detail(false, true, null, true, true));

        mvc.perform(get("/applications/298").with(as("hr_lan", "HR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đang chờ:")))
                .andExpect(content().string(containsString("HR lên lịch phỏng vấn.")))
                .andExpect(content().string(containsString("action=\"/applications/298/review\"")))
                .andExpect(content().string(containsString("name=\"decision\" value=\"Pass\"")))
                .andExpect(content().string(containsString("name=\"decision\" value=\"Hold\"")))
                .andExpect(content().string(containsString("name=\"decision\" value=\"Fail\"")))
                .andExpect(content().string(containsString("action=\"/applications/298/ai-screening\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void reviewSuccessRedirectsBackWithMessage() throws Exception {
        when(reviewService.review(298, new ApplicationReviewRequest("Pass", "Hợp vị trí")))
                .thenReturn("Đã chuyển hồ sơ cho trưởng bộ phận.");

        mvc.perform(post("/applications/298/review").with(as("hr_lan", "HR")).with(csrf())
                        .param("decision", "Pass").param("comments", "Hợp vị trí"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/applications/298"))
                .andExpect(flash().attribute("successMessage", "Đã chuyển hồ sơ cho trưởng bộ phận."));
    }

    @Test
    void businessErrorRendersDetailAgainWithMessage() throws Exception {
        when(reviewService.review(eq(298), any())).thenThrow(
                new ApplicationReviewException(null, "Hồ sơ vừa được người khác xử lý. Vui lòng tải lại trang."));
        when(pipeline.detail(298)).thenReturn(detail(false, true, null, true, false));

        mvc.perform(post("/applications/298/review").with(as("hr_lan", "HR")).with(csrf())
                        .param("decision", "Fail").param("comments", "Thiếu kinh nghiệm"))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/application-detail"))
                .andExpect(content().string(containsString("Hồ sơ vừa được người khác xử lý.")))
                .andExpect(content().string(containsString(">Thiếu kinh nghiệm</textarea>")));
    }

    @Test
    void invalidInputIsRejectedBeforeTheService() throws Exception {
        when(pipeline.detail(298)).thenReturn(detail(false, true, null, true, false));

        mvc.perform(post("/applications/298/review").with(as("hr_lan", "HR")).with(csrf())
                        .param("decision", "Approve").param("comments", "x".repeat(1001)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("reviewForm", "decision", "comments"))
                .andExpect(content().string(containsString("Quyết định không hợp lệ.")))
                .andExpect(content().string(containsString("Nhận xét tối đa 1000 ký tự.")));
        verifyNoInteractions(reviewService);
    }

    @Test
    void reviewNeedsCsrfAndAReviewerRole() throws Exception {
        mvc.perform(post("/applications/298/review").with(as("hr_lan", "HR")).param("decision", "Pass"))
                .andExpect(status().isForbidden());
        when(reviewService.review(eq(298), any())).thenThrow(new AccessDeniedException("no"));
        mvc.perform(post("/applications/298/review").with(as("director", "Director")).with(csrf())
                        .param("decision", "Pass"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/applications/298/review").with(as("phong.nguyen", "Candidate")).with(csrf())
                        .param("decision", "Pass"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rescreenShowsNewScoreOrReason() throws Exception {
        when(reviewService.rescreen(293)).thenReturn(new AiScreeningOutcome(new BigDecimal("71.2")));
        when(reviewService.rescreen(120)).thenThrow(
                new ApplicationReviewException(null, "Chỉ chấm lại AI khi hồ sơ chưa qua vòng nhân sự."));

        mvc.perform(post("/applications/293/ai-screening").with(as("hr_lan", "HR")).with(csrf()))
                .andExpect(redirectedUrl("/applications/293"))
                .andExpect(flash().attribute("successMessage", "Đã chấm lại AI: 71,20 điểm."));
        mvc.perform(post("/applications/120/ai-screening").with(as("hr_lan", "HR")).with(csrf()))
                .andExpect(redirectedUrl("/applications/120"))
                .andExpect(flash().attribute("errorMessage", "Chỉ chấm lại AI khi hồ sơ chưa qua vòng nhân sự."));
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

    private static ApplicationDetailResponse detail(boolean nextSteps, boolean openJobPosting, String linkedIn) {
        return detail(nextSteps, openJobPosting, linkedIn, false, false);
    }

    private static ApplicationDetailResponse detail(boolean nextSteps, boolean openJobPosting, String linkedIn,
                                                    boolean review, boolean rescreen) {
        return new ApplicationDetailResponse(298, "An Võ", "an.vo@example.com", "0901 234 567", linkedIn, null,
                7, "REQ-007", 12, "Digital Marketing Executive", "Sales & Marketing", SUBMITTED,
                "HM_Passed", "Qua vòng chuyên môn", "badge--warning", new BigDecimal("79.34"), SUBMITTED.plusMinutes(1),
                List.of(new TimelineItem(SUBMITTED.plusDays(1), "HR: Đạt", "Lan HR", "Hợp vị trí", "success"),
                        new TimelineItem(SUBMITTED, "Nộp hồ sơ", "An Võ", null, "neutral")),
                "HR lên lịch phỏng vấn.",
                new ApplicationDetailResponse.Actions(review, rescreen, nextSteps, nextSteps, openJobPosting),
                openJobPosting ? "HR" : "Hiring Manager");
    }

    private static ApplicationPipelineResponse row(int id, String status, String score, boolean schedule,
                                                   boolean offer) {
        return new ApplicationPipelineResponse(id, "An Võ", "an.vo@example.com", "Digital Marketing Executive",
                SUBMITTED, status, score == null ? null : new BigDecimal(score)).withActions(schedule, offer);
    }
}
