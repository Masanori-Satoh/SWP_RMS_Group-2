package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.ApplicationReviewRequest;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.ApplicationReview;
import com.group2.rms.candidate.exception.ApplicationReviewException;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.repository.ApplicationReviewRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Duyệt hồ sơ và chấm lại AI. {@link ApplicationAccess} là mock (luật quyền đã có test riêng);
 * ở đây kiểm tra bảng chuyển trạng thái, lượt duyệt được ghi, và các nhánh từ chối.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationReviewServiceTests {

    private static final int ID = 298;

    @Mock ApplicationRepository applications;
    @Mock ApplicationReviewRepository reviews;
    @Mock ApplicationAccess access;
    @Mock AiScreeningService aiScreeningService;

    private ApplicationReviewService service;
    private final User actor = User.builder().userId(1).fullName("Lan HR").build();

    @BeforeEach
    void setUp() {
        service = new ApplicationReviewService(applications, reviews, access, aiScreeningService);
        when(access.actor()).thenReturn(actor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Applied", "AI_Screened"})
    void hrPassForwardsToHiringManager(String status) {
        Application application = reviewable(status);
        when(applications.transition(ID, ApplicationAccess.HR_REVIEW_STATUSES, "HR_Passed")).thenReturn(1);

        String message = service.review(ID, new ApplicationReviewRequest("Pass", "  Hợp vị trí  "));

        ApplicationReview saved = savedReview();
        assertSame(application, saved.getApplication());
        assertSame(actor, saved.getReviewer());
        assertEquals("HR", saved.getReviewerRole());
        assertEquals("Pass", saved.getDecision());
        assertEquals("Hợp vị trí", saved.getComments());
        assertEquals("Đã chuyển hồ sơ cho trưởng bộ phận.", message);
    }

    @Test
    void hiringManagerPassApprovesForInterview() {
        reviewable("HR_Passed");
        when(applications.transition(ID, Set.of("HR_Passed"), "HM_Passed")).thenReturn(1);

        String message = service.review(ID, new ApplicationReviewRequest("Pass", null));

        assertEquals("HiringManager", savedReview().getReviewerRole());
        assertEquals("Đã duyệt hồ sơ. HR có thể lên lịch phỏng vấn.", message);
    }

    @Test
    void failRejectsWithOptionalComment() {
        reviewable("HR_Passed");
        when(applications.transition(ID, Set.of("HR_Passed"), "Rejected")).thenReturn(1);

        service.review(ID, new ApplicationReviewRequest("Fail", "   "));

        assertEquals("Fail", savedReview().getDecision());
        assertNull(savedReview().getComments(), "nhận xét rỗng lưu null");
    }

    @Test
    void holdRecordsReviewButKeepsStatus() {
        reviewable("AI_Screened");

        assertEquals("Đã tạm giữ hồ sơ.", service.review(ID, new ApplicationReviewRequest("Hold", "Chờ bổ sung")));

        assertEquals("Hold", savedReview().getDecision());
        verify(applications, never()).transition(anyInt(), any(), any());
    }

    @Test
    void someoneElseMovedTheApplicationFirst() {
        reviewable("AI_Screened");
        when(applications.transition(anyInt(), any(), any())).thenReturn(0);

        ApplicationReviewException e = assertThrows(ApplicationReviewException.class,
                () -> service.review(ID, new ApplicationReviewRequest("Pass", null)));

        assertNull(e.getField());
        assertTrue(e.getMessage().contains("người khác vừa") || e.getMessage().contains("vừa được người khác"));
    }

    @Test
    void directorAndAdminCannotReview() {
        Application application = application("AI_Screened");
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));
        when(access.isReviewer(actor)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.review(ID, new ApplicationReviewRequest("Pass", null)));
        verifyNoInteractions(reviews);
    }

    @Test
    void reviewerAtTheWrongStepGetsAFormMessage() {
        Application application = application("HR_Passed");
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));
        when(access.isReviewer(actor)).thenReturn(true);
        when(access.canReview(actor, application)).thenReturn(false);

        ApplicationReviewException e = assertThrows(ApplicationReviewException.class,
                () -> service.review(ID, new ApplicationReviewRequest("Pass", null)));

        assertTrue(e.getMessage().contains("Qua vòng nhân sự"), e.getMessage());
        verifyNoInteractions(reviews);
        verify(applications, never()).transition(anyInt(), any(), any());
    }

    @Test
    void reviewOutsideScopeIsDenied() {
        Application application = application("HR_Passed");
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));
        doThrow(new AccessDeniedException("no")).when(access).requireView(actor, application);

        assertThrows(AccessDeniedException.class, () -> service.review(ID, new ApplicationReviewRequest("Pass", null)));
        verifyNoInteractions(reviews);
    }

    @Test
    void reviewOfMissingApplicationIsNotFound() {
        when(applications.findDetailById(ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.review(ID, new ApplicationReviewRequest("Pass", null)));
    }

    // ------------------------------------------------------------------ chấm lại AI

    @Test
    void hrRescreensBeforeForwarding() {
        Application application = application("Applied");
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));
        when(access.isHr(actor)).thenReturn(true);
        when(access.canRescreen(actor, application)).thenReturn(true);
        when(aiScreeningService.screen(ID)).thenReturn(new AiScreeningOutcome(new BigDecimal("71.20")));

        assertEquals(new BigDecimal("71.20"), service.rescreen(ID).matchScore());
    }

    @Test
    void onlyHrMayRescreen() {
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application("Applied")));
        when(access.isHr(actor)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.rescreen(ID));
        verifyNoInteractions(aiScreeningService);
    }

    @Test
    void rescreenAfterForwardingIsRefused() {
        Application application = application("HR_Passed");
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));
        when(access.isHr(actor)).thenReturn(true);
        when(access.canRescreen(actor, application)).thenReturn(false);

        assertThrows(ApplicationReviewException.class, () -> service.rescreen(ID));
        verifyNoInteractions(aiScreeningService);
    }

    private Application reviewable(String status) {
        Application application = application(status);
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));
        when(access.isReviewer(actor)).thenReturn(true);
        when(access.canReview(actor, application)).thenReturn(true);
        return application;
    }

    private ApplicationReview savedReview() {
        ArgumentCaptor<ApplicationReview> captor = ArgumentCaptor.forClass(ApplicationReview.class);
        verify(reviews, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private static Application application(String status) {
        return Application.builder().applicationId(ID).applicationStatus(status).build();
    }
}
