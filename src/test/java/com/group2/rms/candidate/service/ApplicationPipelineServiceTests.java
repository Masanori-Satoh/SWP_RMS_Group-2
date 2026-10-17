package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.ApplicationDetailResponse;
import com.group2.rms.candidate.dto.ApplicationListResponse;
import com.group2.rms.candidate.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.dto.ApplicationSearch;
import com.group2.rms.candidate.entity.AIScreeningResult;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.ApplicationReview;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.candidate.repository.AIScreeningResultRepository;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.repository.ApplicationRepository.ProgressFactsRow;
import com.group2.rms.candidate.repository.ApplicationRepository.TimelineEventRow;
import com.group2.rms.candidate.repository.ApplicationReviewRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.web.StageItem;
import com.group2.rms.core.web.TimelineItem;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Đọc danh sách và chi tiết hồ sơ; repository và {@link ApplicationAccess} là mock.
 * Câu query thật được kiểm ở {@code ApplicationPipelineQueryTests} trên DB local.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationPipelineServiceTests {

    private static final int ID = 298;
    private static final LocalDateTime SUBMITTED = LocalDateTime.of(2026, 10, 10, 14, 59);

    @Mock ApplicationRepository applications;
    @Mock AIScreeningResultRepository screeningResults;
    @Mock ApplicationReviewRepository reviews;
    @Mock ApplicationAccess access;
    @Mock CvStorage cvStorage;
    @Mock JobPostingRepository jobPostings;

    private ApplicationPipelineService service;
    private final User actor = User.builder().userId(1).build();

    @BeforeEach
    void setUp() {
        service = new ApplicationPipelineService(applications, screeningResults, reviews, access, cvStorage,
                jobPostings);
        when(access.actor()).thenReturn(actor);
    }

    @Test
    void listPassesViewerScopeAndCleanedFilterToRepository() {
        when(access.scopeOf(actor)).thenReturn(ApplicationScope.hiringManager(5, 10));
        when(applications.findPipelineIds(anyBoolean(), any(), any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(List.of(ID));
        when(applications.findProgressFacts(List.of(ID))).thenReturn(List.of(facts("HM_Passed", null)));
        when(applications.findPipelineRows(List.of(ID))).thenReturn(List.of(row("HM_Passed")));
        when(access.canScheduleInterview(eq(actor), argThat(p -> p.stage() == ApplicationStage.INTERVIEW)))
                .thenReturn(true);

        ApplicationListResponse result = service.list(
                new ApplicationSearch(6, "Bogus", "  an.vo  ", "newest", "Bogus"),
                PageRequest.of(0, 20, Sort.by("applicationId")));

        verify(applications).findPipelineIds(false, 5, 10, true, 6, null, "an.vo", "newest");
        verify(applications).findPostingOptions(false, 5, 10, true);
        assertTrue(result.forwardedView());
        assertEquals(new ApplicationSearch(6, null, "an.vo", "newest", null), result.search());
        ApplicationPipelineResponse first = result.applications().getContent().getFirst();
        assertTrue(first.canScheduleInterview());
        assertFalse(first.canCreateOffer());
        assertEquals("Chờ xếp lịch phỏng vấn", first.statusLabel());
        assertEquals("badge--warning", first.statusBadge());
    }

    /** Nghiệm thu PR 1 dòng 3: đạt phỏng vấn, chưa có offer thì mới có "Tạo offer", không còn "Lên lịch". */
    @Test
    void listLabelsAndActionsFollowInterviewResult() {
        when(access.scopeOf(actor)).thenReturn(ApplicationScope.ALL);
        when(applications.findPipelineIds(anyBoolean(), any(), any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(List.of(ID));
        when(applications.findProgressFacts(List.of(ID))).thenReturn(List.of(facts("Interviewing", "Passed")));
        when(applications.findPipelineRows(List.of(ID))).thenReturn(List.of(row("Interviewing")));
        when(access.canCreateOffer(eq(actor), argThat(p -> p.step() == ApplicationProgressStep.PASSED_AWAITING_OFFER)))
                .thenReturn(true);

        ApplicationPipelineResponse first = service.list(new ApplicationSearch(null, null, null, null, null),
                PageRequest.of(0, 20)).applications().getContent().getFirst();

        assertEquals("Đạt phỏng vấn, chờ lập offer", first.statusLabel());
        assertFalse(first.canScheduleInterview());
        assertTrue(first.canCreateOffer());
    }

    /**
     * Nghiệm thu PR 4: đếm theo giai đoạn dùng chung Resolver với nhãn, nên đơn {@code HM_Passed} đã chốt Không đạt
     * (dữ liệu cũ) nằm ở "Đã dừng", không ở "Phỏng vấn"; tổng các giai đoạn bằng số đơn.
     */
    @Test
    void stageCountsUseResolvedProgressNotStoredStatus() {
        when(access.scopeOf(actor)).thenReturn(ApplicationScope.ALL);
        when(applications.findPipelineIds(anyBoolean(), any(), any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(List.of(1, 2, 3, 4, 5));
        when(applications.findProgressFacts(List.of(1, 2, 3, 4, 5))).thenReturn(List.of(
                facts(1, "Applied", null), facts(2, "HM_Passed", null), facts(3, "HM_Passed", "Failed"),
                facts(4, "Interviewing", "Passed"), facts(5, "Rejected", null)));

        ApplicationListResponse result = service.list(new ApplicationSearch(null, null, null, null, null),
                PageRequest.of(0, 20));

        assertEquals(Map.of(ApplicationStage.APPLIED, 1L, ApplicationStage.SCREENING, 0L,
                ApplicationStage.INTERVIEW, 1L, ApplicationStage.OFFER, 1L, ApplicationStage.HIRED, 0L,
                ApplicationStage.CLOSED, 2L), result.stageCounts());
        assertEquals(5, result.totalCount());
        assertEquals(5, result.applications().getTotalElements());
    }

    /** Lọc giai đoạn giữ thứ tự của câu query, cắt trang trong Java; số đếm vẫn của mọi giai đoạn. */
    @Test
    void stageFilterKeepsQueryOrderAndPagesInJava() {
        when(access.scopeOf(actor)).thenReturn(ApplicationScope.ALL);
        when(applications.findPipelineIds(anyBoolean(), any(), any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(List.of(5, 4, 3, 2, 1));
        when(applications.findProgressFacts(List.of(5, 4, 3, 2, 1))).thenReturn(List.of(
                facts(1, "HM_Passed", null), facts(2, "Applied", null), facts(3, "Interviewing", null),
                facts(4, "Applied", null), facts(5, "HM_Passed", null)));
        when(applications.findPipelineRows(List.of(1))).thenReturn(List.of(row(1, "HM_Passed")));

        ApplicationListResponse result = service.list(
                new ApplicationSearch(null, null, null, null, "INTERVIEW"), PageRequest.of(1, 2));

        assertEquals(List.of(1), result.applications().getContent().stream()
                .map(ApplicationPipelineResponse::applicationId).toList(), "trang 2 của [5, 3, 1] với 2 dòng/trang");
        assertEquals(3, result.applications().getTotalElements());
        assertEquals(2, result.applications().getTotalPages());
        assertEquals(5, result.totalCount());
        assertEquals(ApplicationStage.INTERVIEW, result.search().stageFilter());
    }

    /** Repository trả dòng không theo thứ tự nào; trang hiện đúng thứ tự id của câu query. */
    @Test
    void pageRowsFollowIdOrder() {
        when(access.scopeOf(actor)).thenReturn(ApplicationScope.ALL);
        when(applications.findPipelineIds(anyBoolean(), any(), any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(List.of(3, 1, 2));
        when(applications.findProgressFacts(List.of(3, 1, 2))).thenReturn(List.of(
                facts(1, "Applied", null), facts(2, "Applied", null), facts(3, "Applied", null)));
        when(applications.findPipelineRows(List.of(3, 1, 2)))
                .thenReturn(List.of(row(1, "Applied"), row(2, "Applied"), row(3, "Applied")));

        List<Integer> order = service.list(new ApplicationSearch(null, null, null, null, null), PageRequest.of(0, 20))
                .applications().getContent().stream().map(ApplicationPipelineResponse::applicationId).toList();

        assertEquals(List.of(3, 1, 2), order);
    }

    /** SQL Server giới hạn ~2100 tham số: dữ kiện đọc theo lô 2000 id. */
    @Test
    void progressFactsAreReadInBatchesOf2000() {
        List<Integer> ids = IntStream.rangeClosed(1, 2001).boxed().toList();
        when(access.scopeOf(actor)).thenReturn(ApplicationScope.ALL);
        when(applications.findPipelineIds(anyBoolean(), any(), any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(ids);
        when(applications.findProgressFacts(any())).thenReturn(List.of());

        service.list(new ApplicationSearch(null, null, null, null, null), PageRequest.of(0, 20));

        verify(applications).findProgressFacts(ids.subList(0, 2000));
        verify(applications).findProgressFacts(List.of(2001));
    }

    @Test
    void emptyListSkipsProgressAndRowQueries() {
        when(access.scopeOf(actor)).thenReturn(ApplicationScope.ALL);
        when(applications.findPipelineIds(anyBoolean(), any(), any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(List.of());

        ApplicationListResponse result = service.list(new ApplicationSearch(null, null, null, null, null),
                PageRequest.of(0, 20));

        verify(applications, never()).findProgressFacts(any());
        verify(applications, never()).findPipelineRows(any());
        assertEquals(0, result.totalCount());
        assertTrue(result.applications().isEmpty());
    }

    /** Nghiệm thu PR 4: lọc theo tin thì có tên tin cho tiêu đề, kể cả tin chưa có hồ sơ nào. */
    @Test
    void postingFilterLooksUpPostingTitle() {
        when(access.scopeOf(actor)).thenReturn(ApplicationScope.ALL);
        when(access.canOpenJobPosting(actor)).thenReturn(true);
        when(applications.findPipelineIds(anyBoolean(), any(), any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(jobPostings.findById(12)).thenReturn(Optional.of(
                JobPosting.builder().jobPostingId(12).postingTitle("Digital Marketing Executive").build()));

        ApplicationListResponse result = service.list(new ApplicationSearch(12, null, null, null, null),
                PageRequest.of(0, 20));

        assertEquals("Digital Marketing Executive", result.postingTitle());
        assertTrue(result.canOpenJobPosting());
    }

    @Test
    void withoutPostingFilterThereIsNoPostingTitle() {
        when(access.scopeOf(actor)).thenReturn(ApplicationScope.ALL);
        when(applications.findPipelineIds(anyBoolean(), any(), any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(List.of());

        assertNull(service.list(new ApplicationSearch(null, null, null, null, null), PageRequest.of(0, 20))
                .postingTitle());
        verifyNoInteractions(jobPostings);
    }

    @Test
    void unknownSortFallsBackToAiScore() {
        when(access.scopeOf(actor)).thenReturn(ApplicationScope.ALL);
        when(applications.findPipelineIds(anyBoolean(), any(), any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(List.of());

        ApplicationListResponse result = service.list(new ApplicationSearch(null, null, " ", "salary", null),
                PageRequest.of(0, 20));

        verify(applications).findPipelineIds(true, null, null, false, null, null, null, "score");
        assertFalse(result.forwardedView());
    }

    @Test
    void detailOfMissingApplicationIsNotFound() {
        when(applications.findDetailById(ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.detail(ID));
    }

    @Test
    void detailOutsideViewerScopeIsDeniedBeforeReadingAnythingElse() {
        Application application = application();
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));
        doThrow(new AccessDeniedException("no")).when(access).requireView(actor, application);

        assertThrows(AccessDeniedException.class, () -> service.detail(ID));
        verifyNoInteractions(screeningResults, reviews);
    }

    @Test
    void detailMapsLatestScoreActionsAndTimelineNewestFirst() {
        Application application = application();
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));
        when(screeningResults.findByApplication_ApplicationIdOrderByAiScreeningIdDesc(ID)).thenReturn(List.of(
                score("79.34", SUBMITTED.plusMinutes(1)), score("60.00", SUBMITTED.plusSeconds(5))));
        when(reviews.findWithReviewerByApplicationId(ID)).thenReturn(List.of(ApplicationReview.builder()
                .reviewerRole("HR").decision("Pass").comments("Hợp vị trí")
                .reviewer(User.builder().fullName("Lan HR").build())
                .reviewedAt(SUBMITTED.plusDays(1)).build()));
        when(applications.findInterviewEvents(ID)).thenReturn(List.of(event(SUBMITTED.plusDays(3), "Scheduled")));
        when(applications.findInterviewResultEvents(ID)).thenReturn(List.of(event(SUBMITTED.plusDays(4), "Passed")));
        when(applications.findOfferEvents(ID)).thenReturn(List.of(event(SUBMITTED.plusDays(5), "Draft")));
        when(access.canReview(actor, application)).thenReturn(true);
        when(access.canOpenJobPosting(actor)).thenReturn(true);

        ApplicationDetailResponse detail = service.detail(ID);

        assertEquals("An Võ", detail.fullName());
        assertEquals("REQ-007", detail.requisitionCode());
        assertEquals("Sales & Marketing", detail.departmentName());
        assertEquals(new BigDecimal("79.34"), detail.aiMatchScore());
        assertEquals("Chờ HR duyệt CV", detail.statusLabel());
        assertEquals(List.of("done", "current", "upcoming", "upcoming", "upcoming"),
                detail.stepper().stream().map(StageItem::state).toList());
        assertEquals("Chờ HR duyệt CV · Đang chờ: HR", detail.stepper().get(1).detail());
        assertNull(detail.interviewId(), "chưa có lịch phỏng vấn");
        assertEquals(new ApplicationDetailResponse.Actions(true, false, false, false, true), detail.actions());
        assertEquals(List.of("Offer: Draft", "Kết quả phỏng vấn: Đạt", "Lịch phỏng vấn: Scheduled", "HR: Đạt",
                        "AI chấm điểm: 79,34", "AI chấm điểm: 60,00", "Nộp hồ sơ"),
                detail.timeline().stream().map(TimelineItem::title).toList());
        assertEquals("success", detail.timeline().get(1).tone());
        TimelineItem review = detail.timeline().get(3);
        assertEquals("Lan HR", review.actor());
        assertEquals("Hợp vị trí", review.body());
        assertEquals("success", review.tone());
    }

    /** Đã chốt Đạt, chưa có offer: stepper đứng ở Offer và trang chi tiết biết buổi phỏng vấn để mở kết quả. */
    @Test
    void pendingHmSuggestionDoesNotClaimTheCandidateSubmittedAnApplication() {
        Application suggestion = application();
        suggestion.setApplicationStatus("HM_Suggested");
        when(applications.findDetailById(ID)).thenReturn(Optional.of(suggestion));

        ApplicationDetailResponse detail = service.detail(ID);

        assertEquals("HM đề xuất — chờ HR liên hệ", detail.statusLabel());
        assertEquals(1, detail.timeline().size());
        TimelineItem proposed = detail.timeline().getFirst();
        assertEquals("HM đề xuất", proposed.title());
        assertNull(proposed.actor(), "Không gán hành động đề xuất cho ứng viên hoặc bịa tên HM");
        assertEquals(SUBMITTED, proposed.at());
        assertTrue(proposed.body().contains("ứng viên đồng ý"));
        assertTrue(detail.timeline().stream().noneMatch(item -> "Nộp hồ sơ".equals(item.title())));
        verify(applications, never()).save(any());
        verify(applications, never()).saveAndFlush(any());
    }

    @Test
    void acceptedSuggestionShowsActualHmAndHrEventsInsteadOfInventingCandidateSubmission() {
        var a = application(); a.setApplicationStatus("HM_Passed");
        when(applications.findDetailById(ID)).thenReturn(Optional.of(a));
        var proposed = mock(ApplicationRepository.TalentPoolEventRow.class);
        when(proposed.getKind()).thenReturn("TalentPoolSuggested");
        when(proposed.getAt()).thenReturn(SUBMITTED);
        when(proposed.getActor()).thenReturn("HM Tuấn");
        var accepted = mock(ApplicationRepository.TalentPoolEventRow.class);
        when(accepted.getKind()).thenReturn("TalentPoolActivated");
        when(accepted.getAt()).thenReturn(SUBMITTED.plusDays(1));
        when(accepted.getActor()).thenReturn("HR Lan");
        when(applications.findTalentPoolEvents(ID)).thenReturn(List.of(accepted, proposed));
        var detail = service.detail(ID);
        assertFalse(detail.proposalOnly());
        assertEquals(List.of("HR xác nhận ứng viên đồng ý dùng CV cũ", "HM đề xuất"), detail.timeline().stream().map(TimelineItem::title).toList());
        assertEquals("HR Lan", detail.timeline().getFirst().actor());
        assertEquals("Chờ xếp lịch phỏng vấn", detail.statusLabel());
    }

    @Test
    void cancelledDraftRemainsAProposalRatherThanARejectedOfficialApplication() {
        var a = application(); a.setApplicationStatus("Rejected");
        when(applications.findDetailById(ID)).thenReturn(Optional.of(a));
        when(applications.countTalentPoolCancellation(ID)).thenReturn(1);
        var ended = mock(ApplicationRepository.TalentPoolEventRow.class);
        when(ended.getKind()).thenReturn("TalentPoolCancelled");
        when(ended.getAt()).thenReturn(SUBMITTED);
        when(ended.getActor()).thenReturn("HR Lan");
        when(applications.findTalentPoolEvents(ID)).thenReturn(List.of(ended));
        var detail = service.detail(ID);
        assertTrue(detail.proposalOnly());
        assertEquals("Đề xuất đã kết thúc", detail.statusLabel());
        assertEquals("Kết thúc đề xuất", detail.timeline().getFirst().title());
    }

    @Test
    void detailStepperFollowsProgressAndExposesLatestInterviewId() {
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application()));
        when(screeningResults.findByApplication_ApplicationIdOrderByAiScreeningIdDesc(ID)).thenReturn(List.of());
        when(applications.findProgressFacts(List.of(ID))).thenReturn(List.of(facts("Interviewing", "Passed")));

        ApplicationDetailResponse detail = service.detail(ID);

        assertEquals(77L, detail.interviewId());
        assertEquals(List.of("done", "done", "done", "current", "upcoming"),
                detail.stepper().stream().map(StageItem::state).toList());
        assertEquals("Đạt phỏng vấn, chờ lập offer · Đang chờ: HR", detail.stepper().get(3).detail());
    }

    /** Nghiệm thu PR 1 dòng 2: chốt Không đạt hiện trên timeline màu đỏ. */
    @Test
    void failedInterviewResultIsDangerOnTimeline() {
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application()));
        when(screeningResults.findByApplication_ApplicationIdOrderByAiScreeningIdDesc(ID)).thenReturn(List.of());
        when(applications.findInterviewResultEvents(ID)).thenReturn(List.of(event(SUBMITTED.plusDays(4), "Failed")));

        TimelineItem result = service.detail(ID).timeline().getFirst();

        assertEquals("Kết quả phỏng vấn: Không đạt", result.title());
        assertEquals("danger", result.tone());
    }

    @Test
    void detailWithoutAiScoreLeavesScoreEmpty() {
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application()));
        when(screeningResults.findByApplication_ApplicationIdOrderByAiScreeningIdDesc(ID)).thenReturn(List.of());

        ApplicationDetailResponse detail = service.detail(ID);

        assertNull(detail.aiMatchScore());
        assertNull(detail.screenedAt());
        assertEquals(List.of("Nộp hồ sơ"), detail.timeline().stream().map(TimelineItem::title).toList());
    }

    // ------------------------------------------------------------------ CV

    @Test
    void cvStoredByWebSubmissionIsLoadedFromStorage() {
        Application application = application();
        application.setAppliedCvUrl("local:cv/15/abc.pdf");
        Resource pdf = new ByteArrayResource("%PDF-1.7".getBytes());
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));
        when(cvStorage.load("cv/15/abc.pdf")).thenReturn(pdf);

        ApplicationCv cv = service.cv(ID);

        assertSame(pdf, cv.file());
        assertNull(cv.redirectUrl());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/samples/cv/sample_cv_accountant.pdf", "https://res.cloudinary.com/x/cv.pdf"})
    void sampleOrExternalCvIsRedirected(String url) {
        Application application = application();
        application.setAppliedCvUrl(url);
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));

        assertEquals(ApplicationCv.redirect(url), service.cv(ID));
        verifyNoInteractions(cvStorage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"//evil.example/cv.pdf", "javascript:alert(1)", "s3://bucket/cv.pdf"})
    void unknownCvLocationIsNotFoundInsteadOfRedirecting(String url) {
        Application application = application();
        application.setAppliedCvUrl(url);
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));

        assertThrows(ResourceNotFoundException.class, () -> service.cv(ID));
    }

    @Test
    void cvOutsideViewerScopeIsDeniedBeforeTouchingStorage() {
        Application application = application();
        application.setAppliedCvUrl("local:cv/15/abc.pdf");
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));
        doThrow(new AccessDeniedException("no")).when(access).requireView(actor, application);

        assertThrows(AccessDeniedException.class, () -> service.cv(ID));
        verifyNoInteractions(cvStorage);
    }

    private static ApplicationPipelineResponse row(String status) {
        return row(ID, status);
    }

    private static ApplicationPipelineResponse row(int id, String status) {
        return new ApplicationPipelineResponse(id, "An Võ", "an.vo@example.com", "Digital Marketing Executive",
                SUBMITTED, status, new BigDecimal("79.34"));
    }

    private static Application application() {
        JobRequisition requisition = JobRequisition.builder().requisitionId(7).requisitionCode("REQ-007")
                .department(Department.builder().departmentId(20).departmentName("Sales & Marketing").build())
                .build();
        return Application.builder()
                .applicationId(ID)
                .candidate(Candidate.builder().account(User.builder().fullName("An Võ").build()).build())
                .jobPosting(JobPosting.builder().jobPostingId(12).postingTitle("Digital Marketing Executive")
                        .requisition(requisition).build())
                .submissionDate(SUBMITTED)
                .applicationStatus("AI_Screened")
                .build();
    }

    private static AIScreeningResult score(String value, LocalDateTime at) {
        return AIScreeningResult.builder().aiMatchScore(new BigDecimal(value)).screenedAt(at).build();
    }

    private static ProgressFactsRow facts(String status, String decision) {
        return facts(ID, status, decision);
    }

    /** Dữ kiện của một đơn: {@code decision == null} là chưa có lịch; có thì là lịch đã xong, đủ phiếu. */
    private static ProgressFactsRow facts(int id, String status, String decision) {
        boolean interviewed = decision != null;
        return new ProgressFactsRow() {
            @Override public Integer getApplicationId() { return id; }
            @Override public String getApplicationStatus() { return status; }
            @Override public Boolean getWithdrawn() { return false; }
            @Override public Long getInterviewId() { return interviewed ? 77L : null; }
            @Override public String getInterviewStatus() { return interviewed ? "Completed" : null; }
            @Override public LocalDateTime getInterviewStart() { return interviewed ? SUBMITTED.plusDays(3) : null; }
            @Override public Integer getPanelCount() { return interviewed ? 2 : 0; }
            @Override public Integer getEvaluationCount() { return interviewed ? 2 : 0; }
            @Override public String getFinalDecision() { return decision; }
            @Override public String getOfferStatus() { return null; }
        };
    }

    private static TimelineEventRow event(LocalDateTime at, String status) {
        return new TimelineEventRow() {
            @Override
            public LocalDateTime getAt() {
                return at;
            }

            @Override
            public String getStatus() {
                return status;
            }
        };
    }
}
