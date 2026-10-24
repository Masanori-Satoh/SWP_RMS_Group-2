package com.group2.rms.candidate;

import com.group2.rms.candidate.controller.ApplicationPipelineController;
import com.group2.rms.candidate.dto.ApplicationDetailResponse;
import com.group2.rms.candidate.dto.ApplicationListResponse;
import com.group2.rms.candidate.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.dto.ApplicationReviewRequest;
import com.group2.rms.candidate.dto.ApplicationSearch;
import com.group2.rms.candidate.dto.JobPostingOption;
import com.group2.rms.candidate.exception.ApplicationReviewException;
import com.group2.rms.candidate.screening.AiScreeningOutcome;
import com.group2.rms.candidate.cv.ApplicationCv;
import com.group2.rms.candidate.service.ApplicationPipelineService;
import com.group2.rms.candidate.service.ApplicationReviewService;
import com.group2.rms.candidate.service.ApplicationStage;
import com.group2.rms.candidate.service.ApplicationStatusLabels;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.core.web.StageItem;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
@WebMvcTest({ApplicationPipelineController.class, com.group2.rms.candidate.controller.TalentPoolController.class})
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class ApplicationPipelineWebTests {

    private static final LocalDateTime SUBMITTED = LocalDateTime.of(2026, 10, 10, 14, 59);

    @Autowired MockMvc mvc;
    @MockitoBean ApplicationPipelineService pipeline;
    @MockitoBean ApplicationReviewService reviewService;
    @MockitoBean com.group2.rms.candidate.service.TalentPoolService talentPool;
    @MockitoBean com.group2.rms.candidate.service.RecruitmentEmailService recruitmentEmail;
    @MockitoBean UserRepository users;

    private static com.group2.rms.candidate.dto.TalentPoolRecommendationResponse recommendation(boolean pending, boolean decide) {
        return new com.group2.rms.candidate.dto.TalentPoolRecommendationResponse(219,15,8,"Tin đích","An Võ","HR Lan",SUBMITTED,
                pending ? "Chờ phản hồi" : "HM đã chấp nhận",pending,decide);
    }

    @Test void hrRecommendationHubLinksToExistingSelectionAndConfirmsWithdrawal() throws Exception {
        when(talentPool.recommendations(8)).thenReturn(List.of(recommendation(true,false)));
        when(talentPool.viewerRole()).thenReturn("HR"); when(talentPool.canRecommend(8)).thenReturn(true);
        mvc.perform(get("/talent-pool/jobs/8/recommendations").with(as("hr","HR")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Gợi ý ứng viên cho HM")))
                .andExpect(content().string(containsString("href=\"/talent-pool?jobPostingId=8\"")))
                .andExpect(content().string(containsString("data-pool-withdraw-dialog=\"tp-withdraw-dialog\"")))
                .andExpect(content().string(containsString("id=\"tp-withdraw-dialog\"")))
                .andExpect(content().string(containsString("/talent-pool/jobs/8/recommendations/219/withdraw")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        when(talentPool.canRecommend(8)).thenReturn(false);
        when(talentPool.recommendations(8)).thenReturn(List.of(recommendation(false,false)));
        mvc.perform(get("/talent-pool/jobs/8/recommendations").with(as("hr","HR")))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString("Gợi ý ứng viên cho HM"))))
                .andExpect(content().string(not(containsString("data-pool-withdraw-dialog"))));
        when(talentPool.viewerRole()).thenReturn("Hiring Manager");
        when(talentPool.recommendations(8)).thenReturn(List.of(recommendation(true,true)));
        mvc.perform(get("/talent-pool/jobs/8/recommendations").with(as("hm","Hiring Manager")))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString("data-pool-withdraw-dialog"))))
                .andExpect(content().string(containsString("data-pool-decision")));
    }

    @Test void withdrawalEnforcesHrAndCsrfBeforeCallingService() throws Exception {
        for(String role: List.of("Hiring Manager","Candidate","Director","System Admin"))
            mvc.perform(post("/talent-pool/jobs/8/recommendations/219/withdraw").with(as("other",role)).with(csrf()))
                    .andExpect(status().isForbidden());
        mvc.perform(post("/talent-pool/jobs/8/recommendations/219/withdraw").with(as("hr","HR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(talentPool);
    }

    @Test void withdrawalPreservesSearchContextAndReportsRaceFailure() throws Exception {
        mvc.perform(post("/talent-pool/jobs/8/recommendations/219/withdraw?keyword=An&page=2&returnUrl=https://example.org")
                        .with(as("hr","HR")).with(csrf()))
                .andExpect(redirectedUrl("/talent-pool/jobs/8/recommendations?keyword=An&page=2"))
                .andExpect(flash().attributeExists("successMessage"));
        verify(talentPool).withdrawRecommendation(8,219);
        doThrow(new com.group2.rms.candidate.exception.TalentPoolException("Gợi ý đã được xử lý."))
                .when(talentPool).withdrawRecommendation(8,219);
        mvc.perform(post("/talent-pool/jobs/8/recommendations/219/withdraw").with(as("hr","HR")).with(csrf()))
                .andExpect(redirectedUrl("/talent-pool/jobs/8/recommendations?page=0"))
                .andExpect(flash().attribute("errorMessage","Gợi ý đã được xử lý."))
                .andExpect(flash().attributeCount(1));
    }

    @Test void hrReusesSelectionWithItsOwnEndpointAndManagerContext() throws Exception {
        when(talentPool.search(any(),any())).thenReturn(new PageImpl<>(List.of()));
        when(talentPool.viewerRole()).thenReturn("HR"); when(talentPool.targetJobTitle(8)).thenReturn("Tin đích");
        when(talentPool.receivingManager(8)).thenReturn("HM Tuấn");
        mvc.perform(get("/talent-pool?jobPostingId=8").with(as("hr","HR")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("data-pool-flow=\"hr\"")))
                .andExpect(content().string(containsString("action=\"/talent-pool/jobs/8/recommend\"")))
                .andExpect(content().string(containsString("Gửi gợi ý cho HM")))
                .andExpect(content().string(containsString("HM phụ trách: HM Tuấn")))
                .andExpect(content().string(containsString("id=\"tp-selection-bar\"")));
    }

    @Test void recommendationSubmitUsesHrOnlyAndNoFakeSuccessOnFailure() throws Exception {
        mvc.perform(post("/talent-pool/jobs/8/recommend").with(as("hr","HR")).with(csrf())
                        .contentType("application/json").content("{\"sourceApplicationIds\":[101]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(talentPool).recommend(8,new com.group2.rms.candidate.dto.TalentPoolSuggestRequest(List.of(101)));
        doThrow(new com.group2.rms.candidate.exception.TalentPoolException("Đã có gợi ý.")).when(talentPool).recommend(eq(8),any());
        mvc.perform(post("/talent-pool/jobs/8/recommend").with(as("hr","HR")).with(csrf())
                        .contentType("application/json").content("{\"sourceApplicationIds\":[101]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        verify(talentPool,never()).suggest(anyInt(),any());
    }

    @Test void flowCEndpointsEnforceRolesCsrfAndSourceValidation() throws Exception {
        for (String role : List.of("Candidate","Hiring Manager","Director"))
            mvc.perform(post("/talent-pool/jobs/8/recommend").with(as("other",role)).with(csrf())
                    .contentType("application/json").content("{\"sourceApplicationIds\":[101]}"))
                    .andExpect(status().isForbidden());
        mvc.perform(post("/talent-pool/jobs/8/recommend").with(as("hr","HR"))
                .contentType("application/json").content("{\"sourceApplicationIds\":[101]}"))
                .andExpect(status().isForbidden());
        for (String body : List.of("{\"sourceApplicationIds\":[]}","{\"sourceApplicationIds\":[-1]}"))
            mvc.perform(post("/talent-pool/jobs/8/recommend").with(as("hr","HR")).with(csrf())
                    .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        mvc.perform(post("/talent-pool/recommendations/219/decision").with(as("hr","HR")).with(csrf()).param("decision","Pass"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/talent-pool/recommendations/219/decision").with(as("hm","Hiring Manager")).param("decision","Fail"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(talentPool);
    }

    @Test void recommendationCardsUseFileUserNavigationAndCompactDecisionForms() throws Exception {
        when(talentPool.recommendations(8)).thenReturn(List.of(recommendation(true,true)));
        when(talentPool.targetJobTitle(8)).thenReturn("Tin đích"); when(talentPool.viewerRole()).thenReturn("Hiring Manager");
        mvc.perform(get("/talent-pool/jobs/8/recommendations?keyword=An").with(as("hm","Hiring Manager")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("1 chờ phản hồi")))
                .andExpect(content().string(containsString("data-icon=\"file-user\"")))
                .andExpect(content().string(containsString("/talent-pool/15?recommendationId=219")))
                .andExpect(content().string(containsString("/talent-pool/recommendations/219/decision")))
                .andExpect(content().string(not(containsString("data-app-preview"))))
                .andExpect(content().string(containsString("HR Lan")));
    }

    @Test void recommendationDetailShowsAllCvNotesAndSharedDecisionAction() throws Exception {
        var c = new com.group2.rms.candidate.dto.TalentPoolCandidateResponse(15,"An Võ","an@example.com","0901234567",List.of());
        var history = List.of(new com.group2.rms.candidate.dto.TalentPoolCvResponse(101,"Java Backend",SUBMITTED),
                new com.group2.rms.candidate.dto.TalentPoolCvResponse(102,"QA Automation",SUBMITTED));
        when(talentPool.recommendation(219,15)).thenReturn(recommendation(true,true));
        when(talentPool.recommendationDetail(15,219)).thenReturn(new com.group2.rms.candidate.dto.TalentPoolDetailResponse(c,true,
                List.of(new com.group2.rms.candidate.dto.TalentPoolNoteResponse(1,"HR Lan","<script>note</script>",SUBMITTED)),history));
        mvc.perform(get("/talent-pool/15?recommendationId=219&keyword=An&page=1").with(as("hm","Hiring Manager")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Quay lại gợi ý từ HR")))
                .andExpect(content().string(containsString("Java Backend"))).andExpect(content().string(containsString("QA Automation")))
                .andExpect(content().string(containsString("/talent-pool/15/cv/101"))).andExpect(content().string(containsString("/talent-pool/15/cv/102")))
                .andExpect(content().string(containsString("&lt;script&gt;note&lt;/script&gt;")))
                .andExpect(content().string(containsString("Quyết định của Hiring Manager")))
                .andExpect(content().string(containsString("/talent-pool/recommendations/219/decision")))
                .andExpect(content().string(containsString("recommendationId=219")));
    }

    @Test void decisionsShareOneServiceValidateAndPreserveTypedListContext() throws Exception {
        when(talentPool.recommendationForDecision(219)).thenReturn(recommendation(true,true));
        when(talentPool.decideRecommendation(219,true)).thenReturn(8);
        mvc.perform(post("/talent-pool/recommendations/219/decision?keyword=An&page=1&returnUrl=https://example.org")
                        .with(as("hm","Hiring Manager")).with(csrf()).param("decision","Pass"))
                .andExpect(redirectedUrl("/talent-pool/jobs/8/recommendations?keyword=An&page=1"))
                .andExpect(flash().attributeExists("successMessage"));
        mvc.perform(post("/talent-pool/recommendations/219/decision").with(as("hm","Hiring Manager")).with(csrf()).param("decision","INVALID"))
                .andExpect(flash().attributeExists("errorMessage"));
        verify(talentPool,times(1)).decideRecommendation(eq(219),anyBoolean());
        doThrow(new com.group2.rms.candidate.exception.TalentPoolException("Đã xử lý.")).when(talentPool).decideRecommendation(219,false);
        mvc.perform(post("/talent-pool/recommendations/219/decision").with(as("hm","Hiring Manager")).with(csrf()).param("decision","Fail"))
                .andExpect(flash().attribute("errorMessage","Đã xử lý."));
    }

    @Test void pendingRecommendationCannotUseOrdinaryApplicationDetailAndWrongScopeIsDenied() throws Exception {
        when(talentPool.pendingRecommendationUrl(219)).thenReturn("/talent-pool/15?recommendationId=219");
        mvc.perform(get("/applications/219").with(as("hr","HR"))).andExpect(redirectedUrl("/talent-pool/15?recommendationId=219"));
        when(talentPool.recommendation(219,15)).thenThrow(new AccessDeniedException("Wrong scope"));
        mvc.perform(get("/talent-pool/15?recommendationId=219").with(as("other","Hiring Manager"))).andExpect(status().isForbidden());
        verifyNoInteractions(pipeline);
    }

    @Test
    void bookmarkModalPreservesListActionsAndCsrf() throws Exception {
        when(pipeline.list(any(), any())).thenReturn(listOf(true, "Hiring Manager",
                row(218, "HM_Passed", "80", true, true)));
        mvc.perform(get("/applications").with(as("hm_tuan", "Hiring Manager")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-app-preview")))
                .andExpect(content().string(containsString("data-bookmark-url=\"/talent-pool/applications/218/bookmark\"")))
                .andExpect(content().string(containsString("Xem chi tiết")))
                .andExpect(content().string(containsString("/interviews/new?applicationId=218")))
                .andExpect(content().string(containsString("/offers/create?applicationId=218")))
                .andExpect(content().string(containsString("Lịch sử ghi chú")))
                .andExpect(content().string(containsString("Bỏ lưu tiềm năng")))
                .andExpect(content().string(not(containsString("more-menu"))))
                .andExpect(content().string(not(containsString("Tối đa 1.000 ký tự."))))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void bookmarkRemovalUsesExistingServiceForHrAndHm() throws Exception {
        when(talentPool.removeBookmark(218)).thenReturn(
                new com.group2.rms.candidate.dto.TalentPoolBookmarkResponse(8, false, List.of()));
        for (String role : List.of("HR", "Hiring Manager")) {
            mvc.perform(post("/talent-pool/applications/218/bookmark/remove").with(as("reviewer", role)).with(csrf()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(false));
        }
        verify(talentPool, times(2)).removeBookmark(218);
        verifyNoInteractions(reviewService, recruitmentEmail, pipeline);
    }

    @Test
    void bookmarkRemovalRequiresCsrfRoleAndApplicationAccess() throws Exception {
        mvc.perform(post("/talent-pool/applications/218/bookmark/remove").with(as("hm_tuan", "Hiring Manager")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/talent-pool/applications/218/bookmark/remove").with(as("candidate", "Candidate")).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(talentPool);
        when(talentPool.removeBookmark(218)).thenThrow(new AccessDeniedException("Forbidden"));
        mvc.perform(post("/talent-pool/applications/218/bookmark/remove").with(as("hm_other", "Hiring Manager")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void bookmarkReadAndSaveUseExistingService() throws Exception {
        var state = new com.group2.rms.candidate.dto.TalentPoolBookmarkResponse(8, true, List.of());
        when(talentPool.bookmark(218)).thenReturn(state);
        when(talentPool.saveBookmark(eq(218), any())).thenReturn(state);
        mvc.perform(get("/talent-pool/applications/218/bookmark").with(as("hm_tuan", "Hiring Manager")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true));
        mvc.perform(post("/talent-pool/applications/218/bookmark").with(as("hr_lan", "HR")).with(csrf())
                        .param("content", "Nhận xét chung"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.candidateId").value(8));
        verify(talentPool).saveBookmark(218, new com.group2.rms.candidate.dto.TalentPoolBookmarkRequest("Nhận xét chung"));
        verifyNoInteractions(reviewService, recruitmentEmail);
    }

    @Test
    void bookmarkRequiresRoleCsrfAndValidLength() throws Exception {
        mvc.perform(get("/talent-pool/applications/218/bookmark").with(as("candidate", "Candidate")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/talent-pool/applications/218/bookmark").with(as("hm_tuan", "Hiring Manager")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/talent-pool/applications/218/bookmark").with(as("hm_tuan", "Hiring Manager")).with(csrf())
                        .param("content", "x".repeat(1001)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        verifyNoInteractions(talentPool);
    }

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
                .andExpect(content().string(not(containsString("Hồ sơ của phòng ban bạn"))))
                .andExpect(content().string(containsString("href=\"/applications/298\"")))
                .andExpect(content().string(containsString("data-cv-url=\"/applications/298/cv\"")))
                .andExpect(content().string(containsString(">79<")))
                .andExpect(content().string(containsString("Chờ AI chấm")))
                .andExpect(content().string(containsString("badge--warning")))
                .andExpect(content().string(containsString("href=\"/interviews/new?applicationId=120\"")))
                .andExpect(content().string(containsString("href=\"/offers/create?applicationId=120\"")))
                .andExpect(content().string(not(containsString("applicationId=298"))))
                .andExpect(content().string(not(containsString("class=\"more-menu\""))))
                .andExpect(content().string(containsString("Đang chờ: HR")))
                .andExpect(content().string(not(containsString("name=\"status\""))));
    }

    /** Nghiệm thu PR 4: ô giai đoạn có số đếm, giữ bộ lọc đang có trên link, đánh dấu ô đang lọc. */
    @Test
    void stageBarKeepsFiltersAndMarksSelectedStage() throws Exception {
        Map<ApplicationStage, Long> counts = zeroCounts();
        counts.put(ApplicationStage.INTERVIEW, 4L);
        counts.put(ApplicationStage.CLOSED, 2L);
        ApplicationSearch search = new ApplicationSearch(6, null, "Võ", "newest", "INTERVIEW").normalized();
        when(pipeline.list(any(), any())).thenReturn(new ApplicationListResponse(
                new PageImpl<>(List.of(row(120, "HM_Passed", "88.10", true, false)), Pageable.ofSize(20), 4),
                counts, List.of(), search, null, true, false, "HR"));

        MvcResult result = mvc.perform(get("/applications").with(as("hr_lan", "HR")).param("stage", "INTERVIEW"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"Lọc theo giai đoạn\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("class=\"stage-bar__divider\"")))
                .andExpect(content().string(containsString("name=\"stage\" value=\"INTERVIEW\"")))
                .andExpect(content().string(containsString("Xóa bộ lọc")))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<StageItem> items = (List<StageItem>) result.getModelAndView().getModel().get("stageItems");
        assertEquals(List.of("ALL", "APPLIED", "SCREENING", "INTERVIEW", "OFFER", "HIRED", "CLOSED"),
                items.stream().map(StageItem::key).toList());
        assertEquals(List.of(6, 0, 0, 4, 0, 0, 2), items.stream().map(StageItem::count).toList());
        assertEquals(List.of(true, false, false, false, false, false, true),
                items.stream().map(StageItem::offFlow).toList());
        assertEquals(List.of("upcoming", "upcoming", "upcoming", "current", "upcoming", "upcoming", "upcoming"),
                items.stream().map(StageItem::state).toList());
        assertEquals("/applications?jobPostingId=6&keyword=V%C3%B5&sort=newest", items.get(0).href());
        assertEquals("/applications?jobPostingId=6&keyword=V%C3%B5&sort=newest&stage=CLOSED", items.get(6).href());
    }

    @Test
    void hiringManagerSeesDepartmentTitleAndEmptyState() throws Exception {
        when(pipeline.list(any(), any())).thenReturn(listOf(true, "Hiring Manager"));

        mvc.perform(get("/applications").with(as("hm_khanh", "Hiring Manager")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("viewerRole", "Hiring Manager"))
                .andExpect(content().string(containsString("Hồ sơ của phòng ban bạn")))
                .andExpect(content().string(containsString("bạn duyệt hồ sơ HR đã chuyển")))
                .andExpect(content().string(containsString("Chưa có hồ sơ nào nộp vào phòng ban của bạn.")))
                .andExpect(content().string(not(containsString("Hồ sơ được chuyển"))))
                .andExpect(content().string(not(containsString("<table"))));
    }

    /** Nghiệm thu PR 4: lọc theo tin thì tiêu đề là tên tin; HR có link quay lại trang tin, HM thì không. */
    @Test
    void postingFilterShowsPostingTitleAndBackLinkOnlyWhenAllowed() throws Exception {
        ApplicationSearch search = new ApplicationSearch(6, null, null, "score", null).normalized();
        when(pipeline.list(any(), any())).thenReturn(new ApplicationListResponse(
                new PageImpl<>(List.of(), Pageable.ofSize(20), 0), zeroCounts(), List.of(), search,
                "Digital Marketing Executive", true, false, "HR"));

        mvc.perform(get("/applications").with(as("hr_lan", "HR")).param("jobPostingId", "6"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hồ sơ cho tin: Digital Marketing Executive")))
                .andExpect(content().string(not(containsString("Danh sách ứng viên"))))
                .andExpect(content().string(containsString("href=\"/internal/job-postings/6\"")));

        when(pipeline.list(any(), any())).thenReturn(new ApplicationListResponse(
                new PageImpl<>(List.of(), Pageable.ofSize(20), 0), zeroCounts(), List.of(), search,
                "Digital Marketing Executive", false, true, "Hiring Manager"));

        mvc.perform(get("/applications").with(as("hm_tuan", "Hiring Manager")).param("jobPostingId", "6"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hồ sơ cho tin: Digital Marketing Executive")))
                .andExpect(content().string(containsString("Mọi hồ sơ nộp vào tin này; bạn duyệt hồ sơ HR đã chuyển.")))
                .andExpect(content().string(not(containsString("/internal/job-postings/6"))));
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
                        .param("keyword", "an.vo").param("sort", "newest").param("stage", "INTERVIEW")
                        .param("page", "2"))
                .andExpect(status().isOk());

        // "sort" cũng bị Spring Data đọc vào Pageable; service bỏ qua sort của Pageable nên chỉ so trang + cỡ trang.
        verify(pipeline).list(eq(new ApplicationSearch(6, "Rejected", "an.vo", "newest", "INTERVIEW")),
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
                .andExpect(content().string(containsString("src=\"/applications/298/cv#view=FitH&amp;navpanes=0\"")))
                .andExpect(content().string(containsString(">79,34/100<")))
                .andExpect(content().string(containsString("HR: Đạt")))
                .andExpect(content().string(containsString("Hợp vị trí")))
                .andExpect(content().string(containsString("href=\"https://linkedin.com/in/an-vo\"")))
                .andExpect(content().string(containsString("href=\"/interviews/new?applicationId=298\"")))
                .andExpect(content().string(containsString("href=\"/interviews/77/result\"")))
                .andExpect(content().string(containsString("href=\"/offers/create?applicationId=298\"")))
                .andExpect(content().string(containsString("aria-current=\"step\"")))
                .andExpect(content().string(containsString("data-state=\"done\"")));
    }

    @Test
    void pendingSuggestionShowsConsentNoticeInsteadOfFormalApplicationProgress() throws Exception {
        when(pipeline.detail(298)).thenReturn(detail(false, true, null, false, false, null, "HM_Suggested"));

        mvc.perform(get("/applications/298").with(as("hr_lan", "HR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ứng viên HM đề xuất")))
                .andExpect(content().string(containsString("href=\"/talent-pool/jobs/12/suggestions\"")))
                .andExpect(content().string(containsString("Đề xuất #298")))
                .andExpect(content().string(containsString("Đề xuất đang chờ ứng viên đồng ý.")))
                .andExpect(content().string(containsString("Hành trình đề xuất")))
                .andExpect(content().string(containsString("Chưa sàng lọc AI")))
                .andExpect(content().string(containsString("aria-label=\"Tiến độ đề xuất\"")))
                .andExpect(content().string(not(containsString(">Sàng lọc CV<"))))
                .andExpect(content().string(not(containsString("stage-bar--stepper"))))
                .andExpect(content().string(not(containsString(">Nộp</span>"))))
                .andExpect(content().string(not(containsString("id=\"app-actions-title\""))));
    }

    @Test
    void detailWorkspacePlacesContactNotesAndActivityBesideCvAndAiNearJourney() throws Exception {
        when(pipeline.detail(298)).thenReturn(detail(false, true, "https://linkedin.com/in/an-vo", false, true, null));
        String html = mvc.perform(get("/applications/298").with(as("hr_lan", "HR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"mailto:an.vo@example.com\"")))
                .andExpect(content().string(containsString("href=\"tel:0901 234 567\"")))
                .andExpect(content().string(containsString("action=\"/applications/298/ai-screening\"")))
                .andExpect(content().string(containsString("Hành trình hoạt động")))
                .andExpect(content().string(not(containsString("id=\"app-ai-title\""))))
                .andReturn().getResponse().getContentAsString();
        int journey = html.indexOf("id=\"app-journey-title\"");
        int cv = html.indexOf("id=\"app-cv-title\"");
        int side = html.indexOf("<aside class=\"app-detail__side\"");
        int contact = html.indexOf("id=\"app-contact-title\"");
        int notes = html.indexOf("id=\"app-detail-notes\"");
        int history = html.indexOf("id=\"app-timeline-title\"");
        assertTrue(journey >= 0 && journey < cv && cv < side && side < contact && contact < notes && notes < history);
    }

    @Test
    void applicationWithoutAiResultShowsEmptyStateBesideJourney() throws Exception {
        when(pipeline.detail(298)).thenReturn(detail(false, true, null, false, false, null, "Applied", null));
        mvc.perform(get("/applications/298").with(as("hr_lan", "HR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chưa có điểm AI")))
                .andExpect(content().string(containsString("stage-bar--stepper")))
                .andExpect(content().string(not(containsString("Chưa sàng lọc AI"))))
                .andExpect(content().string(not(containsString("<dt>LinkedIn</dt>"))))
                .andExpect(content().string(not(containsString("<dt>Portfolio</dt>"))));
    }

    @Test
    void actionCardIsHiddenWhenViewerHasNothingToDoAndNoInterviewYet() throws Exception {
        when(pipeline.detail(298)).thenReturn(detail(false, true, null, false, false, null));

        mvc.perform(get("/applications/298").with(as("hr_lan", "HR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"Tiến độ hồ sơ\"")))
                .andExpect(content().string(not(containsString("id=\"app-actions-title\""))))
                .andExpect(content().string(not(containsString("Xem kết quả phỏng vấn"))));
    }

    @Test
    void hiringManagerGetsNoLinkToHrOnlyPagesAndUnsafeUrlsAreNotLinked() throws Exception {
        when(pipeline.detail(298)).thenReturn(detail(false, false, "javascript:alert(1)"));

        mvc.perform(get("/applications/298").with(as("hm_huong", "Hiring Manager")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/internal/job-postings/12"))))
                .andExpect(content().string(containsString("Tin Digital Marketing Executive")))
                .andExpect(content().string(not(containsString("href=\"javascript:"))))
                .andExpect(content().string(containsString("aria-label=\"Tiến độ hồ sơ\"")))
                .andExpect(content().string(not(containsString("Bước tiếp theo"))))
                .andExpect(content().string(containsString("href=\"/interviews/77/result\"")))
                .andExpect(content().string(not(containsString("Lên lịch phỏng vấn"))))
                .andExpect(content().string(not(containsString("Tạo offer"))))
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
                .andExpect(content().string(containsString("Chờ xếp lịch phỏng vấn · Đang chờ: HR")))
                .andExpect(content().string(containsString("action=\"/applications/298/review\"")))
                .andExpect(content().string(containsString("name=\"decision\" value=\"Pass\"")))
                .andExpect(content().string(containsString("name=\"decision\" value=\"Hold\"")))
                .andExpect(content().string(containsString("name=\"decision\" value=\"Fail\"")))
                .andExpect(content().string(containsString("action=\"/applications/298/ai-screening\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    /** Q2.1: "Không đạt" mở hộp xác nhận nút đỏ; không JS thì nút vẫn là submit decision=Fail của form duyệt. */
    @Test
    void rejectAsksForConfirmationButStillSubmitsWithoutJavaScript() throws Exception {
        when(pipeline.detail(298)).thenReturn(detail(false, true, null, true, false));

        String html = mvc.perform(get("/applications/298").with(as("hr_lan", "HR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"app-review-form\"")))
                .andExpect(content().string(containsString("data-review-fail")))
                .andExpect(content().string(containsString("id=\"app-reject-dialog\" class=\"confirm-dialog\"")))
                .andExpect(content().string(containsString("Loại ứng viên <span>An Võ</span>?")))
                .andExpect(content().string(containsString(
                        "form=\"app-review-form\" name=\"decision\" value=\"Fail\"")))
                .andExpect(content().string(containsString("Xác nhận loại ứng viên")))
                .andExpect(content().string(containsString("/js/pages/application-review.js")))
                .andReturn().getResponse().getContentAsString();
        // Nút "Không đạt" nằm trong form duyệt (submit thật), hộp xác nhận nằm ngoài form.
        int formStart = html.indexOf("id=\"app-review-form\"");
        int failButton = html.indexOf("data-review-fail");
        int formEnd = html.indexOf("</form>", formStart);
        int dialog = html.indexOf("id=\"app-reject-dialog\"");
        assertTrue(formStart < failButton && failButton < formEnd && formEnd < dialog);
        assertTrue(html.substring(html.lastIndexOf("<button", failButton), failButton)
                .contains("type=\"submit\" name=\"decision\" value=\"Fail\""));

        // Không JS: gửi thẳng decision=Fail vẫn được xử lý như cũ.
        when(reviewService.review(eq(298), argThat(f -> "Fail".equals(f.decision()))))
                .thenReturn("Đã đánh dấu hồ sơ không đạt.");
        mvc.perform(post("/applications/298/review").with(as("hr_lan", "HR")).with(csrf())
                        .param("decision", "Fail").param("comments", "Thiếu kinh nghiệm"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/applications/298"))
                .andExpect(flash().attribute("successMessage", "Đã đánh dấu hồ sơ không đạt."));
    }

    /** Q7.5: người tuyển dụng thấy ngày sinh, giới tính, địa chỉ; trống thì không hiện dòng. */
    @Test
    void detailShowsDateOfBirthGenderAndAddressWhenKnown() throws Exception {
        when(pipeline.detail(298)).thenReturn(detail(false, false, null, false, false, null, "AI_Screened", null,
                "Hiring Manager", true));
        mvc.perform(get("/applications/298").with(as("hm_tuan", "Hiring Manager")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thông tin ứng viên")))
                .andExpect(content().string(containsString("<dt>Ngày sinh</dt><dd>17/08/1995</dd>")))
                .andExpect(content().string(containsString("<dt>Giới tính</dt><dd>Nữ</dd>")))
                .andExpect(content().string(containsString("<dt>Địa chỉ</dt><dd>Số 88 Láng Hạ, Đống Đa, Hà Nội</dd>")));

        when(pipeline.detail(298)).thenReturn(detail(false, false, null, false, false, null, "AI_Screened", null,
                "Hiring Manager", false));
        mvc.perform(get("/applications/298").with(as("hm_tuan", "Hiring Manager")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<dt>Ngày sinh</dt>"))))
                .andExpect(content().string(not(containsString("<dt>Giới tính</dt>"))))
                .andExpect(content().string(not(containsString("<dt>Địa chỉ</dt>"))))
                .andExpect(content().string(containsString("<dt>Email</dt>")));
    }

    @Test
    void readOnlyViewerGetsNoRejectDialog() throws Exception {
        when(pipeline.detail(298)).thenReturn(detail(false, true, null, false, false));

        mvc.perform(get("/applications/298").with(as("hr_lan", "HR")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("app-reject-dialog"))))
                .andExpect(content().string(not(containsString("Xác nhận loại ứng viên"))));
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
                .andExpect(content().string(containsString("<span class=\"form-error\">Độ dài phải từ 0 đến 1,000 ký tự.</span>")));
        verifyNoInteractions(reviewService);
    }

    @Test
    void reviewNeedsCsrfAndAReviewerRole() throws Exception {
        mvc.perform(post("/applications/298/review").with(as("hr_lan", "HR")).param("decision", "Pass"))
                .andExpect(status().isForbidden());
        // Admin qua được rule URL nhưng Service từ chối (Director nay là người duyệt thay, xem test dưới).
        when(reviewService.review(eq(298), any())).thenThrow(new AccessDeniedException("no"));
        mvc.perform(post("/applications/298/review").with(as("admin", "System Admin")).with(csrf())
                        .param("decision", "Pass"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/applications/298/review").with(as("phong.nguyen", "Candidate")).with(csrf())
                        .param("decision", "Pass"))
                .andExpect(status().isForbidden());
    }

    /** Q2.3: Director thấy form duyệt ở HR_Passed kèm dòng nhắc "duyệt thay"; HR, HM không thấy dòng này. */
    @Test
    void directorReviewsOnBehalfOfHiringManager() throws Exception {
        when(pipeline.detail(298)).thenReturn(
                detail(false, false, null, true, false, null, "HR_Passed", null, "Director"));
        mvc.perform(get("/applications/298").with(as("director", "Director")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bạn đang duyệt thay trưởng bộ phận")))
                .andExpect(content().string(containsString("name=\"decision\" value=\"Pass\"")))
                .andExpect(content().string(containsString("name=\"decision\" value=\"Fail\"")));

        when(pipeline.detail(298)).thenReturn(
                detail(false, false, null, true, false, null, "HR_Passed", null, "Hiring Manager"));
        mvc.perform(get("/applications/298").with(as("hm_tuan", "Hiring Manager")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Bạn đang duyệt thay trưởng bộ phận"))));

        when(reviewService.review(eq(298), any()))
                .thenReturn("Đã duyệt thay trưởng bộ phận. HR có thể lên lịch phỏng vấn.");
        mvc.perform(post("/applications/298/review").with(as("director", "Director")).with(csrf())
                        .param("decision", "Pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/applications/298"))
                .andExpect(flash().attribute("successMessage",
                        "Đã duyệt thay trưởng bộ phận. HR có thể lên lịch phỏng vấn."));
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
    @Test
    void poolSelectionShowsSourcesNotesAndReadOnlyConfirmationWithoutLegacySubmit() throws Exception {
        var first = new com.group2.rms.candidate.dto.TalentPoolCvResponse(101, "Vị trí nguồn đầy đủ", SUBMITTED);
        var second = new com.group2.rms.candidate.dto.TalentPoolCvResponse(102, "Vị trí nguồn thứ hai", SUBMITTED);
        var candidate = new com.group2.rms.candidate.dto.TalentPoolCandidateResponse(15, "An Võ", "an@example.com", null, List.of(first, second));
        var missing = new com.group2.rms.candidate.dto.TalentPoolCandidateResponse(16, "Chưa có CV", "no-cv@example.com", null, List.of());
        when(talentPool.search(any(), any())).thenReturn(new PageImpl<>(List.of(candidate, missing), org.springframework.data.domain.PageRequest.of(1, 20), 51));
        when(talentPool.viewerRole()).thenReturn("Hiring Manager");
        when(talentPool.selectionScope()).thenReturn(5);
        when(talentPool.targetJobTitle(8)).thenReturn("Tin đích");
        mvc.perform(get("/talent-pool?jobPostingId=8&keyword=An&page=1")
                        .with(as("hm", "Hiring Manager")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chọn ứng viên tiềm năng")))
                .andExpect(content().string(containsString("data-pool-scope=\"5\"")))
                .andExpect(content().string(containsString("id=\"tp-select-page\"")))
                .andExpect(content().string(containsString("Chưa có CV nguồn")))
                .andExpect(content().string(containsString("data-eligible=\"false\"")))
                .andExpect(content().string(containsString("/talent-pool/15/cv/101")))
                .andExpect(content().string(containsString("/talent-pool/15/cv/102")))
                .andExpect(content().string(containsString("data-notes-url=\"/talent-pool/15/notes\"")))
                .andExpect(content().string(containsString("/talent-pool/jobs/8/selection-review")))
                .andExpect(content().string(containsString("action=\"/talent-pool/jobs/8/suggest\"")))
                .andExpect(content().string(containsString("keyword=An")))
                .andExpect(content().string(containsString("jobPostingId=8")))
                .andExpect(content().string(containsString("id=\"keyword\"")))
                .andExpect(content().string(containsString("id=\"markedBy\"")))
                .andExpect(content().string(containsString("id=\"sourceJobId\"")))
                .andExpect(content().string(not(containsString("chưa sẵn sàng"))));
        verify(talentPool, never()).suggest(anyInt(), any());
    }

    @Test
    void globalPoolHasNoDestinationSelectionButStillShowsNotesAndCvActions() throws Exception {
        when(talentPool.search(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(talentPool.viewerRole()).thenReturn("HR");
        mvc.perform(get("/talent-pool").with(as("hr", "HR")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"tp-selection-bar\""))))
                .andExpect(content().string(not(containsString("id=\"tp-selection-form\""))))
                .andExpect(content().string(containsString("id=\"tp-notes\"")));
    }

    @Test
    void hmSubmitsSuggestionsAsJsonAndGetsSuccessOnlyAfterServiceReturns() throws Exception {
        mvc.perform(post("/talent-pool/jobs/8/suggest").with(as("hm", "Hiring Manager")).with(csrf())
                        .contentType("application/json").content("{\"sourceApplicationIds\":[101,102]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.addedCount").value(2)).andExpect(jsonPath("$.data.jobPostingId").value(8));
        verify(talentPool).suggest(8, new com.group2.rms.candidate.dto.TalentPoolSuggestRequest(List.of(101, 102)));
        verifyNoInteractions(recruitmentEmail, reviewService);
    }

    @Test
    void suggestionSubmissionRejectsMissingCsrfWrongRoleAndInvalidSources() throws Exception {
        mvc.perform(post("/talent-pool/jobs/8/suggest").with(as("hm", "Hiring Manager"))
                        .contentType("application/json").content("{\"sourceApplicationIds\":[101]}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/talent-pool/jobs/8/suggest").with(as("hr", "HR")).with(csrf())
                        .contentType("application/json").content("{\"sourceApplicationIds\":[101]}"))
                .andExpect(status().isForbidden());
        for (String body : List.of("{\"sourceApplicationIds\":[]}", "{\"sourceApplicationIds\":[-1]}",
                "{\"sourceApplicationIds\":[" + String.join(",", java.util.Collections.nCopies(21, "101")) + "]}")) {
            mvc.perform(post("/talent-pool/jobs/8/suggest").with(as("hm", "Hiring Manager")).with(csrf())
                            .contentType("application/json").content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        }
        verifyNoInteractions(talentPool);
    }

    @Test
    void suggestionBusinessFailureIsNotReportedAsSuccess() throws Exception {
        doThrow(new com.group2.rms.candidate.exception.TalentPoolException("Ứng viên đã có đề xuất."))
                .when(talentPool).suggest(eq(8), any());
        mvc.perform(post("/talent-pool/jobs/8/suggest").with(as("hm", "Hiring Manager")).with(csrf())
                        .contentType("application/json").content("{\"sourceApplicationIds\":[101]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Ứng viên đã có đề xuất."));
    }

    @Test
    void hrCanReadActualSuggestionListAndOpenTheApplicationForContact() throws Exception {
        when(talentPool.suggestions(8)).thenReturn(List.of(new com.group2.rms.candidate.dto.TalentPoolSuggestionResponse(
                219, "Ứng viên thật", "candidate@example.com", "0901234567", "HM Tuấn", SUBMITTED)));
        when(talentPool.targetJobTitle(8)).thenReturn("Tin tuyển dụng thật");
        when(talentPool.viewerRole()).thenReturn("HR");
        mvc.perform(get("/talent-pool/jobs/8/suggestions").with(as("hr", "HR")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Ứng viên thật")))
                .andExpect(content().string(containsString("href=\"/applications/219\"")))
                .andExpect(content().string(containsString("chưa phải hồ sơ ứng tuyển chính thức")))
                .andExpect(content().string(not(containsString("Thêm ứng viên tiềm năng"))));
        when(talentPool.viewerRole()).thenReturn("Hiring Manager"); when(talentPool.canSuggest(8)).thenReturn(true);
        mvc.perform(get("/talent-pool/jobs/8/suggestions").with(as("hm", "Hiring Manager")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Thêm ứng viên tiềm năng")))
                .andExpect(content().string(containsString("href=\"/talent-pool?jobPostingId=8\"")))
                .andExpect(content().string(containsString("class=\"page-title\">Ứng viên tiềm năng</h1>")));
        when(talentPool.canSuggest(8)).thenReturn(false);
        mvc.perform(get("/talent-pool/jobs/8/suggestions").with(as("hm", "Hiring Manager")))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString("Thêm ứng viên tiềm năng"))));
    }

    @Test
    void readOnlySelectionReviewUsesJsonContractAndCsrf() throws Exception {
        var item = new com.group2.rms.candidate.dto.TalentPoolSelectionReviewResponse.Item(15, 101, "An Võ", "CV nguồn", SUBMITTED, null);
        when(talentPool.reviewSelection(eq(8), any())).thenReturn(new com.group2.rms.candidate.dto.TalentPoolSelectionReviewResponse(List.of(item), true));
        mvc.perform(post("/talent-pool/jobs/8/selection-review").with(as("hm", "Hiring Manager")).with(csrf())
                        .contentType("application/json").content("{\"sourceApplicationIds\":[101]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.submissionSupported").value(true))
                .andExpect(jsonPath("$.data.items[0].candidateId").value(15));
        verify(talentPool).reviewSelection(8, new com.group2.rms.candidate.dto.TalentPoolSuggestRequest(List.of(101)));
        verify(talentPool, never()).suggest(anyInt(), any());
        verifyNoInteractions(recruitmentEmail, reviewService);
    }

    @Test
    void reviewRejectsMissingCsrfInvalidSelectionAndCandidateRole() throws Exception {
        mvc.perform(post("/talent-pool/jobs/8/selection-review").with(as("hm", "Hiring Manager"))
                        .contentType("application/json").content("{\"sourceApplicationIds\":[101]}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/talent-pool/jobs/8/selection-review").with(as("hm", "Hiring Manager")).with(csrf())
                        .contentType("application/json").content("{\"sourceApplicationIds\":[]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(get("/talent-pool/15/notes").with(as("candidate", "Candidate")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(talentPool);
    }

    @Test
    void sharedNotesReadUsesExistingAuthorizedPoolDetail() throws Exception {
        var candidate = new com.group2.rms.candidate.dto.TalentPoolCandidateResponse(15, "An Võ", "an@example.com", null, List.of());
        when(talentPool.detail(15)).thenReturn(new com.group2.rms.candidate.dto.TalentPoolDetailResponse(candidate, true,
                List.of(new com.group2.rms.candidate.dto.TalentPoolNoteResponse(1, "HM Tuấn", "Nhận xét thật", SUBMITTED)), List.of()));
        mvc.perform(get("/talent-pool/15/notes").with(as("hm", "Hiring Manager")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].author").value("HM Tuấn"))
                .andExpect(jsonPath("$.data[0].content").value("Nhận xét thật"));
    }

    @Test
    void talentPoolRendersSelectionAndKeepsCsrf() throws Exception {
        var cv = new com.group2.rms.candidate.dto.TalentPoolCvResponse(10, "Java Developer", SUBMITTED);
        var candidate = new com.group2.rms.candidate.dto.TalentPoolCandidateResponse(15, "An Võ", "an@example.com", "0901234567", List.of(cv));
        when(talentPool.search(any(), any())).thenReturn(new PageImpl<>(List.of(candidate)));
        when(talentPool.viewerRole()).thenReturn("Hiring Manager");
        when(talentPool.targetJobTitle(12)).thenReturn("Java Developer");
        when(talentPool.selectionScope()).thenReturn(5);
        var response = mvc.perform(get("/talent-pool?jobPostingId=12").with(as("hm", "Hiring Manager")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("sourceApplicationIds")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("STT"))).andReturn();
        var path = java.nio.file.Path.of("docs/members/linhdn/tmp/talent-pool/list.html");
        java.nio.file.Files.createDirectories(path.getParent());
        java.nio.file.Files.writeString(path, response.getResponse().getContentAsString(), java.nio.charset.StandardCharsets.UTF_8);
    }
    @Test
    void talentPoolDetailRendersNotesAndCv() throws Exception {
        var cv = new com.group2.rms.candidate.dto.TalentPoolCvResponse(10, "Java Developer", SUBMITTED);
        var c = new com.group2.rms.candidate.dto.TalentPoolCandidateResponse(15, "An Võ", "an@example.com", "0901234567", List.of(cv));
        when(talentPool.detail(15)).thenReturn(new com.group2.rms.candidate.dto.TalentPoolDetailResponse(c, true,
                List.of(new com.group2.rms.candidate.dto.TalentPoolNoteResponse(1, "HR Lan", "Giao tiếp tốt", SUBMITTED)), List.of(cv)));
        when(talentPool.viewerRole()).thenReturn("Hiring Manager");
        when(talentPool.targetJobTitle(12)).thenReturn("Java Developer");
        var response = mvc.perform(get("/talent-pool/15?jobPostingId=12").with(as("hm", "Hiring Manager")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Giao tiếp tốt")))
                .andExpect(content().string(containsString("Quay lại danh sách")))
                .andExpect(content().string(containsString("tp-detail-workspace")))
                .andExpect(content().string(containsString("CV từ các lần ứng tuyển trước")))
                .andExpect(content().string(containsString("Ghi chú đánh giá nội bộ")))
                .andExpect(content().string(containsString("data-cv-url=\"/talent-pool/15/cv/10\"")))
                .andExpect(content().string(containsString("data-hide-detail-link=\"true\"")))
                .andExpect(content().string(not(containsString("talent-pool-selection.js"))))
                .andExpect(content().string(not(containsString("data-pool-source"))))
                .andExpect(content().string(not(containsString("action=\"/talent-pool/jobs/12/suggest\""))))
                .andReturn();
        var path = java.nio.file.Path.of("docs/members/linhdn/tmp/talent-pool/detail.html");
        java.nio.file.Files.createDirectories(path.getParent());
        java.nio.file.Files.writeString(path, response.getResponse().getContentAsString(), java.nio.charset.StandardCharsets.UTF_8);
    }
    @Test
    void talentPoolDetailPreservesTypedContextAndIgnoresExternalReturnUrl() throws Exception {
        var candidate = new com.group2.rms.candidate.dto.TalentPoolCandidateResponse(15, "An Võ", "an@example.com", null, List.of());
        when(talentPool.detail(15)).thenReturn(new com.group2.rms.candidate.dto.TalentPoolDetailResponse(candidate, true, List.of(), List.of()));
        when(talentPool.viewerRole()).thenReturn("Hiring Manager");
        when(talentPool.targetJobTitle(12)).thenReturn("Java Developer");
        mvc.perform(get("/talent-pool/15").with(as("hm", "Hiring Manager"))
                        .param("jobPostingId", "12").param("keyword", "Java Dev").param("markedBy", "5")
                        .param("sourceJobId", "3").param("page", "1").param("returnUrl", "https://example.org"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("backUrl", "/talent-pool?jobPostingId=12&markedBy=5&sourceJobId=3&keyword=Java%20Dev&page=1"))
                .andExpect(content().string(containsString("keyword=Java%20Dev")))
                .andExpect(content().string(containsString("Chưa có CV từ lần ứng tuyển trước.")))
                .andExpect(content().string(containsString("Chưa có ghi chú đánh giá.")))
                .andExpect(content().string(not(containsString("https://example.org"))));
        mvc.perform(get("/talent-pool/15").with(as("hm", "Hiring Manager")))
                .andExpect(status().isOk()).andExpect(model().attribute("backUrl", "/talent-pool"));
    }

    @Test
    void talentPoolNoteRedirectKeepsSearchContextOnSuccessAndValidationFailure() throws Exception {
        String destination = "/talent-pool/15?jobPostingId=12&markedBy=5&sourceJobId=3&keyword=Java%20Dev&page=1";
        mvc.perform(post("/talent-pool/15/notes").with(as("hm", "Hiring Manager")).with(csrf())
                        .param("content", "Ghi chú thật").param("jobPostingId", "12").param("keyword", "Java Dev")
                        .param("markedBy", "5").param("sourceJobId", "3").param("page", "1"))
                .andExpect(redirectedUrl(destination)).andExpect(flash().attribute("successMessage", "Đã thêm ghi chú."));
        verify(talentPool).addNote(eq(15), argThat(note -> note.content().equals("Ghi chú thật")));
        clearInvocations(talentPool);
        String tooLong = "a".repeat(1001);
        mvc.perform(post("/talent-pool/15/notes").with(as("hm", "Hiring Manager")).with(csrf())
                        .param("content", tooLong).param("jobPostingId", "12").param("keyword", "Java Dev")
                        .param("markedBy", "5").param("sourceJobId", "3").param("page", "1"))
                .andExpect(redirectedUrl(destination)).andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attribute("noteDraft", tooLong));
        verifyNoInteractions(talentPool);
        doThrow(new com.group2.rms.candidate.exception.TalentPoolException("Không thể lưu ghi chú."))
                .when(talentPool).addNote(eq(15), any());
        mvc.perform(post("/talent-pool/15/notes").with(as("hm", "Hiring Manager")).with(csrf())
                        .param("content", "Giữ lại nội dung"))
                .andExpect(redirectedUrl("/talent-pool/15"))
                .andExpect(flash().attribute("noteDraft", "Giữ lại nội dung"))
                .andExpect(flash().attribute("errorMessage", "Không thể lưu ghi chú."));
    }

    @Test
    void talentPoolCandidateIsDeniedAndMissingCsrfIsDenied() throws Exception {
        mvc.perform(get("/talent-pool").with(as("candidate", "Candidate"))).andExpect(status().isForbidden());
        mvc.perform(post("/talent-pool/applications/1/confirm").with(as("hr_lan", "HR")).param("consent", "true"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(talentPool);
    }
    @Test
    void hiringManagerCannotConfirmButHrCanConfirmConsent() throws Exception {
        mvc.perform(post("/talent-pool/applications/1/confirm").with(as("hm", "Hiring Manager")).with(csrf()).param("consent","true"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/talent-pool/applications/1/confirm").with(as("hr_lan", "HR")).with(csrf()).param("consent","true"))
                .andExpect(redirectedUrl("/interviews/new?applicationId=1"));
        verify(talentPool).confirm(eq(1), argThat(form -> form.consent()));
    }
    @Test
    void savedClosureShowsMailFailureWithoutRepeatingBusinessAction() throws Exception {
        doThrow(new com.group2.rms.candidate.exception.RecruitmentEmailException()).when(recruitmentEmail).sendJobClosure(12);
        mvc.perform(post("/talent-pool/jobs/12/close").with(as("hr_lan", "HR")).with(csrf()))
                .andExpect(redirectedUrl("/internal/job-postings/12"))
                .andExpect(flash().attribute("errorMessage", containsString("Đã lưu dữ liệu")));
        var order = inOrder(talentPool, recruitmentEmail);
        order.verify(talentPool).closeJob(12);
        order.verify(recruitmentEmail).sendJobClosure(12);
    }

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

    private static ApplicationListResponse listOf(boolean departmentView, String viewerRole,
                                                  ApplicationPipelineResponse... rows) {
        Page<ApplicationPipelineResponse> page = new PageImpl<>(List.of(rows), Pageable.ofSize(20), rows.length);
        return new ApplicationListResponse(page, zeroCounts(),
                List.of(new JobPostingOption(6, "Digital Marketing Executive")),
                new ApplicationSearch(null, null, null, "score", null).normalized(), null, true, departmentView,
                viewerRole);
    }

    private static Map<ApplicationStage, Long> zeroCounts() {
        Map<ApplicationStage, Long> counts = new EnumMap<>(ApplicationStage.class);
        for (ApplicationStage stage : ApplicationStage.values()) {
            counts.put(stage, 0L);
        }
        return counts;
    }

    private static ApplicationDetailResponse detail(boolean nextSteps, boolean openJobPosting, String linkedIn) {
        return detail(nextSteps, openJobPosting, linkedIn, false, false);
    }

    private static ApplicationDetailResponse detail(boolean nextSteps, boolean openJobPosting, String linkedIn,
                                                    boolean review, boolean rescreen) {
        return detail(nextSteps, openJobPosting, linkedIn, review, rescreen, 77L);
    }

    private static ApplicationDetailResponse detail(boolean nextSteps, boolean openJobPosting, String linkedIn,
                                                    boolean review, boolean rescreen, Long interviewId) {
        return detail(nextSteps, openJobPosting, linkedIn, review, rescreen, interviewId, "HM_Passed");
    }

    private static ApplicationDetailResponse detail(boolean nextSteps, boolean openJobPosting, String linkedIn,
                                                    boolean review, boolean rescreen, Long interviewId, String status) {
        return detail(nextSteps, openJobPosting, linkedIn, review, rescreen, interviewId, status,
                "HM_Suggested".equals(status) ? null : new BigDecimal("79.34"));
    }

    private static ApplicationDetailResponse detail(boolean nextSteps, boolean openJobPosting, String linkedIn,
                                                    boolean review, boolean rescreen, Long interviewId, String status, BigDecimal aiMatchScore) {
        return detail(nextSteps, openJobPosting, linkedIn, review, rescreen, interviewId, status, aiMatchScore,
                openJobPosting ? "HR" : "Hiring Manager");
    }

    private static ApplicationDetailResponse detail(boolean nextSteps, boolean openJobPosting, String linkedIn,
                                                    boolean review, boolean rescreen, Long interviewId, String status,
                                                    BigDecimal aiMatchScore, String viewerRole) {
        return detail(nextSteps, openJobPosting, linkedIn, review, rescreen, interviewId, status, aiMatchScore,
                viewerRole, true);
    }

    /** {@code personalFacts}: có ngày sinh, giới tính, địa chỉ (Q7.5) hay để trống. */
    private static ApplicationDetailResponse detail(boolean nextSteps, boolean openJobPosting, String linkedIn,
                                                    boolean review, boolean rescreen, Long interviewId, String status,
                                                    BigDecimal aiMatchScore, String viewerRole, boolean personalFacts) {
        return new ApplicationDetailResponse(298, 15, false, "An Võ", "an.vo@example.com", "0901 234 567", linkedIn, null,
                personalFacts ? java.time.LocalDate.of(1995, 8, 17) : null, personalFacts ? "Nữ" : null,
                personalFacts ? "Số 88 Láng Hạ, Đống Đa, Hà Nội" : "  ",
                7, "REQ-007", 12, "Digital Marketing Executive", "Sales & Marketing", SUBMITTED,
                status, "Qua vòng chuyên môn", "badge--warning", aiMatchScore, aiMatchScore == null ? null : SUBMITTED.plusMinutes(1),
                List.of(new TimelineItem(SUBMITTED.plusDays(1), "HR: Đạt", "Lan HR", "Hợp vị trí", "success"),
                        new TimelineItem(SUBMITTED, "Nộp hồ sơ", "An Võ", null, "neutral")),
                List.of(new StageItem("APPLIED", "Ứng tuyển", null, null, "done", null, false),
                        new StageItem("INTERVIEW", "Phỏng vấn", null, null, "current",
                                "Chờ xếp lịch phỏng vấn · Đang chờ: HR", false)),
                interviewId,
                new ApplicationDetailResponse.Actions(review, rescreen, nextSteps, nextSteps, openJobPosting),
                viewerRole, "HM_Suggested".equals(status));
    }

    private static ApplicationPipelineResponse row(int id, String status, String score, boolean schedule,
                                                   boolean offer) {
        return new ApplicationPipelineResponse(id, "An Võ", "an.vo@example.com", "Digital Marketing Executive",
                SUBMITTED, status, ApplicationStatusLabels.label(status), ApplicationStatusLabels.badge(status),
                "Applied".equals(status) ? null : "HR",
                score == null ? null : new BigDecimal(score), schedule, offer, false);
    }
}
