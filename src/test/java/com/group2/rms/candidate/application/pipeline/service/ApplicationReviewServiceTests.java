package com.group2.rms.candidate.application.pipeline.service;

import com.group2.rms.candidate.domain.rule.ApplicationAccess;
import com.group2.rms.candidate.application.pipeline.dto.ApplicationReviewRequest;
import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.entity.ApplicationReview;
import com.group2.rms.candidate.application.pipeline.exception.ApplicationReviewException;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.candidate.domain.repository.ApplicationReviewRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.notification.service.NotificationService;
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
    @Mock NotificationService notificationService;

    private ApplicationReviewService service;
    private final User actor = User.builder().userId(1).fullName("Lan HR").build();

    @BeforeEach
    void setUp() {
        service = new ApplicationReviewService(applications, reviews, access, notificationService);
        when(access.actor()).thenReturn(actor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Applied"})
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
        verify(notificationService).notifyApplicationAwaitingHiringManager(application, actor);
        verify(notificationService, never()).notifyApplicationReadyForInterview(any(), any());
    }

    @Test
    void hiringManagerPassApprovesForInterview() {
        Application application = reviewable("HR_Passed");
        when(applications.transition(ID, Set.of("HR_Passed"), "HM_Passed")).thenReturn(1);

        String message = service.review(ID, new ApplicationReviewRequest("Pass", null));

        assertEquals("HiringManager", savedReview().getReviewerRole());
        assertEquals("Đã duyệt hồ sơ. HR có thể lên lịch phỏng vấn.", message);
        verify(notificationService).notifyApplicationReadyForInterview(application, actor);
    }

    /** Q2.3: HM vắng thì Director duyệt thay ở bước HM; lượt duyệt ghi vai trò Director (lịch sử hiện "Giám đốc"). */
    @Test
    void directorPassOnBehalfOfHiringManagerIsRecordedAsDirector() {
        reviewable("HR_Passed");
        when(access.isDirector(actor)).thenReturn(true);
        when(applications.transition(ID, Set.of("HR_Passed"), "HM_Passed")).thenReturn(1);

        String message = service.review(ID, new ApplicationReviewRequest("Pass", "HM nghỉ phép"));

        assertEquals("Director", savedReview().getReviewerRole());
        assertEquals("HM nghỉ phép", savedReview().getComments());
        assertEquals("Đã duyệt thay trưởng bộ phận. HR có thể lên lịch phỏng vấn.", message);
    }

    @Test
    void directorFailOnBehalfOfHiringManagerRejects() {
        reviewable("HR_Passed");
        when(access.isDirector(actor)).thenReturn(true);
        when(applications.transition(ID, Set.of("HR_Passed"), "Rejected")).thenReturn(1);

        assertEquals("Đã đánh dấu hồ sơ không đạt.", service.review(ID, new ApplicationReviewRequest("Fail", null)));

        assertEquals("Director", savedReview().getReviewerRole());
        assertEquals("Fail", savedReview().getDecision());
    }

    @Test
    void failRejectsWithOptionalComment() {
        reviewable("HR_Passed");
        when(applications.transition(ID, Set.of("HR_Passed"), "Rejected")).thenReturn(1);

        service.review(ID, new ApplicationReviewRequest("Fail", "   "));

        assertEquals("Fail", savedReview().getDecision());
        assertNull(savedReview().getComments(), "nhận xét rỗng lưu null");
        verifyNoInteractions(notificationService);
    }

    @Test
    void holdRecordsReviewButKeepsStatus() {
        reviewable("Applied");

        assertEquals("Đã tạm giữ hồ sơ.", service.review(ID, new ApplicationReviewRequest("Hold", "Chờ bổ sung")));

        assertEquals("Hold", savedReview().getDecision());
        verify(applications, never()).transition(anyInt(), any(), any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void someoneElseMovedTheApplicationFirst() {
        reviewable("Applied");
        when(applications.transition(anyInt(), any(), any())).thenReturn(0);

        ApplicationReviewException e = assertThrows(ApplicationReviewException.class,
                () -> service.review(ID, new ApplicationReviewRequest("Pass", null)));

        assertNull(e.getField());
        assertTrue(e.getMessage().contains("người khác vừa") || e.getMessage().contains("vừa được người khác"));
    }

    @Test
    void adminAndOtherRolesCannotReview() {
        Application application = application("Applied");
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
