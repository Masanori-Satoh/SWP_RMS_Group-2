package com.group2.rms.candidate.service;

import com.group2.rms.candidate.service.ApplicationReviewService;
import com.group2.rms.candidate.validator.ApplicationAccess;
import com.group2.rms.candidate.dto.ApplicationReviewRequest;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.ApplicationReview;
import com.group2.rms.candidate.exception.ApplicationReviewException;
import com.group2.rms.candidate.repository.ApplicationPipelineRepository;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.repository.ApplicationReviewRepository;
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
    @Mock ApplicationPipelineRepository pipelineQueries;

    private ApplicationReviewService service;
    private final User actor = User.builder().userId(1).fullName("Lan HR").build();

    @BeforeEach
    void setUp() {
        service = new ApplicationReviewService(applications, reviews, access, notificationService, pipelineQueries);
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
        Application application = reviewable("HR_Passed");
        when(access.isDirector(actor)).thenReturn(true);
        when(applications.transition(ID, Set.of("HR_Passed"), "Rejected")).thenReturn(1);

        assertEquals("Đã đánh dấu hồ sơ không đạt.", service.review(ID, new ApplicationReviewRequest("Fail", null)));

        assertEquals("Director", savedReview().getReviewerRole());
        assertEquals("Fail", savedReview().getDecision());
        verify(notificationService).notifyApplicationRejected(application);
    }

    @Test
    void failRejectsWithOptionalComment() {
        Application application = reviewable("HR_Passed");
        when(applications.transition(ID, Set.of("HR_Passed"), "Rejected")).thenReturn(1);

        service.review(ID, new ApplicationReviewRequest("Fail", "   "));

        assertEquals("Fail", savedReview().getDecision());
        assertNull(savedReview().getComments(), "nhận xét rỗng lưu null");
        verify(notificationService).notifyApplicationRejected(application);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void hrFailRejectsAndNotifiesCandidate() {
        Application application = reviewable("Applied");
        when(applications.transition(ID, ApplicationAccess.HR_REVIEW_STATUSES, "Rejected")).thenReturn(1);

        service.review(ID, new ApplicationReviewRequest("Fail", "Chưa đủ kinh nghiệm"));

        verify(notificationService).notifyApplicationRejected(application);
    }

    /** Hai người duyệt cùng lúc: người sau thua thì không báo ứng viên lần nữa. */
    @Test
    void failLosingTheRaceDoesNotNotifyCandidate() {
        reviewable("Applied");
        when(applications.transition(ID, ApplicationAccess.HR_REVIEW_STATUSES, "Rejected")).thenReturn(0);

        assertThrows(ApplicationReviewException.class,
                () -> service.review(ID, new ApplicationReviewRequest("Fail", null)));

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

    // ------------------------------------------------------------------ khôi phục hồ sơ bị loại ở vòng CV

    @Test
    void hrRestoresItsOwnRejectionBackToApplied() {
        Application application = rejectedBy("HR", true);
        when(access.isHr(actor)).thenReturn(true);
        when(applications.transition(ID, Set.of("Rejected"), "Applied")).thenReturn(1);

        assertEquals("Đã khôi phục hồ sơ.", service.restore(ID));

        verify(applications).recordRestoration(1, ID, "RestoredByHR");
        verify(notificationService).notifyApplicationRestored(application);
        verify(reviews, never()).save(any());
        verify(reviews, never()).delete(any());
    }

    @Test
    void hiringManagerRestoresBackToHrPassed() {
        Application application = rejectedBy("HiringManager", true);
        when(applications.transition(ID, Set.of("Rejected"), "HR_Passed")).thenReturn(1);

        service.restore(ID);

        verify(applications).recordRestoration(1, ID, "RestoredByHiringManager");
        verify(notificationService).notifyApplicationRestored(application);
    }

    @Test
    void directorRestoresAHiringManagerRejectionAndIsRecordedAsDirector() {
        rejectedBy("Director", true);
        when(access.isDirector(actor)).thenReturn(true);
        when(applications.transition(ID, Set.of("Rejected"), "HR_Passed")).thenReturn(1);

        service.restore(ID);

        verify(applications).recordRestoration(1, ID, "RestoredByDirector");
    }

    @Test
    void wrongRoleOrDepartmentIsAccessDeniedAndNothingChanges() {
        rejectedBy("HiringManager", false);

        assertThrows(AccessDeniedException.class, () -> service.restore(ID));

        verify(applications, never()).transition(anyInt(), any(), any());
        verify(applications, never()).recordRestoration(anyInt(), anyInt(), any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void nonReviewerRolesCannotRestore() {
        Application application = application("Rejected");
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));
        when(access.isReviewer(actor)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.restore(ID));
        verify(applications, never()).transition(anyInt(), any(), any());
    }

    @Test
    void anApplicationThatCannotBeRestoredShowsTheReasonOnThePage() {
        Application application = application("Rejected");
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));
        when(access.isReviewer(actor)).thenReturn(true);
        when(reviews.findLastFailRole(ID)).thenReturn(null);
        when(access.restoreBlockedReason(application, null, false))
                .thenReturn("Chỉ khôi phục được hồ sơ bị loại ở vòng xem CV.");

        ApplicationReviewException e = assertThrows(ApplicationReviewException.class, () -> service.restore(ID));

        assertNull(e.getField());
        assertEquals("Chỉ khôi phục được hồ sơ bị loại ở vòng xem CV.", e.getMessage());
        verify(applications, never()).transition(anyInt(), any(), any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void aCancelledTalentPoolProposalIsPassedToTheRule() {
        Application application = application("Rejected");
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));
        when(access.isReviewer(actor)).thenReturn(true);
        when(reviews.findLastFailRole(ID)).thenReturn("HR");
        when(pipelineQueries.countTalentPoolCancellation(ID)).thenReturn(1);
        when(access.restoreBlockedReason(application, "HR", true))
                .thenReturn("Đề xuất ứng viên đã bị hủy nên không thể khôi phục hồ sơ.");

        assertThrows(ApplicationReviewException.class, () -> service.restore(ID));
        verify(applications, never()).transition(anyInt(), any(), any());
    }

    @Test
    void twoPeopleRestoringAtOnceTheSecondOneGetsAnErrorAndNoSideEffects() {
        rejectedBy("HR", true);
        when(applications.transition(ID, Set.of("Rejected"), "Applied")).thenReturn(0);

        ApplicationReviewException e = assertThrows(ApplicationReviewException.class, () -> service.restore(ID));

        assertEquals("Hồ sơ vừa được người khác xử lý. Vui lòng tải lại trang.", e.getMessage());
        verify(applications, never()).recordRestoration(anyInt(), anyInt(), any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void restoreOutsideScopeIsDeniedAndMissingApplicationIsNotFound() {
        Application application = application("Rejected");
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));
        doThrow(new AccessDeniedException("no")).when(access).requireView(actor, application);
        assertThrows(AccessDeniedException.class, () -> service.restore(ID));

        when(applications.findDetailById(ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.restore(ID));
    }

    /** Đơn Rejected, lượt Fail mới nhất do {@code failedBy}; {@code allowed} là kết quả {@code canRestore} cho người bấm. */
    private Application rejectedBy(String failedBy, boolean allowed) {
        Application application = application("Rejected");
        when(applications.findDetailById(ID)).thenReturn(Optional.of(application));
        when(access.isReviewer(actor)).thenReturn(true);
        when(reviews.findLastFailRole(ID)).thenReturn(failedBy);
        when(access.canRestore(actor, application, failedBy, false)).thenReturn(allowed);
        return application;
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
