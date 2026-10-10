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
import com.group2.rms.candidate.repository.ApplicationRepository.TimelineEventRow;
import com.group2.rms.candidate.repository.ApplicationReviewRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.web.TimelineItem;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    private ApplicationPipelineService service;
    private final User actor = User.builder().userId(1).build();

    @BeforeEach
    void setUp() {
        service = new ApplicationPipelineService(applications, screeningResults, reviews, access);
        when(access.actor()).thenReturn(actor);
    }

    @Test
    void listPassesViewerScopeAndCleanedFilterToRepository() {
        when(access.scopeOf(actor)).thenReturn(ApplicationScope.hiringManager(5, 10));
        when(applications.searchPipeline(anyBoolean(), any(), any(), anyBoolean(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(row("HM_Passed"))));
        when(access.canScheduleInterview(actor, "HM_Passed")).thenReturn(true);

        ApplicationListResponse result = service.list(new ApplicationSearch(6, "Bogus", "  an.vo  ", "newest"),
                PageRequest.of(2, 20, Sort.by("applicationId")));

        verify(applications).searchPipeline(false, 5, 10, true, 6, null, "an.vo", "newest", PageRequest.of(2, 20));
        verify(applications).findPostingOptions(false, 5, 10, true);
        assertTrue(result.forwardedView());
        assertEquals(new ApplicationSearch(6, null, "an.vo", "newest"), result.search());
        ApplicationPipelineResponse first = result.applications().getContent().getFirst();
        assertTrue(first.canScheduleInterview());
        assertFalse(first.canCreateOffer());
        assertEquals("Qua vòng chuyên môn", first.statusLabel());
    }

    @Test
    void unknownSortFallsBackToAiScore() {
        when(access.scopeOf(actor)).thenReturn(ApplicationScope.ALL);
        when(applications.searchPipeline(anyBoolean(), any(), any(), anyBoolean(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        ApplicationListResponse result = service.list(new ApplicationSearch(null, null, " ", "salary"),
                PageRequest.of(0, 20));

        verify(applications).searchPipeline(true, null, null, false, null, null, null, "score", PageRequest.of(0, 20));
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
        when(applications.findOfferEvents(ID)).thenReturn(List.of(event(SUBMITTED.plusDays(5), "Draft")));
        when(access.canReview(actor, application)).thenReturn(true);
        when(access.canOpenJobPosting(actor)).thenReturn(true);

        ApplicationDetailResponse detail = service.detail(ID);

        assertEquals("An Võ", detail.fullName());
        assertEquals("REQ-007", detail.requisitionCode());
        assertEquals("Sales & Marketing", detail.departmentName());
        assertEquals(new BigDecimal("79.34"), detail.aiMatchScore());
        assertEquals("Đã sàng lọc", detail.statusLabel());
        assertEquals(new ApplicationDetailResponse.Actions(true, false, false, false, true), detail.actions());
        assertEquals(List.of("Offer: Draft", "Lịch phỏng vấn: Scheduled", "HR: Đạt", "AI chấm điểm: 79,34",
                        "AI chấm điểm: 60,00", "Nộp hồ sơ"),
                detail.timeline().stream().map(TimelineItem::title).toList());
        TimelineItem review = detail.timeline().get(2);
        assertEquals("Lan HR", review.actor());
        assertEquals("Hợp vị trí", review.body());
        assertEquals("success", review.tone());
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

    private static ApplicationPipelineResponse row(String status) {
        return new ApplicationPipelineResponse(ID, "An Võ", "an.vo@example.com", "Digital Marketing Executive",
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
