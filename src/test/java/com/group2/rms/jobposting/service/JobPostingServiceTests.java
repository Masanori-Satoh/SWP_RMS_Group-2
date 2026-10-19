package com.group2.rms.jobposting.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.service.TalentPoolService;
import com.group2.rms.core.dto.FilterOption;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse;
import com.group2.rms.jobposting.dto.JobPostingCreateRequest;
import com.group2.rms.jobposting.dto.JobPostingDashboardResponse;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.exception.JobPostingValidationException;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.jobposting.validator.JobPostingValidator;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/*
 * UNIT TEST (Mockito): JobPostingServiceImpl (tạo, sửa, chuyển trạng thái, xóa, chi tiết, bảng điều khiển).
 */
@ExtendWith(MockitoExtension.class)
class JobPostingServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final LocalDate TODAY = NOW.toLocalDate();

    @Mock
    private JobPostingRepository postings;
    @Mock
    private JobRequisitionRepository requisitions;
    @Mock
    private ApplicationRepository applications;
    @Mock
    private AuditLogRepository audit;
    @Mock
    private JobPostingAccess access;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TalentPoolService talentPoolService;

    private JobPostingServiceImpl service;
    private User hr;
    private JobRequisition requisition;

    @BeforeEach
    void setUp() {
        JobPostingValidator validator = new JobPostingValidator(CLOCK);
        JobPostingMapper mapper = new JobPostingMapper(access, validator);
        JobPostingAuditLogger auditLogger = new JobPostingAuditLogger(audit, CLOCK);
        service = new JobPostingServiceImpl(postings, requisitions, applications, access, validator, mapper,
                auditLogger, notificationService, talentPoolService, CLOCK);

        hr = User.builder().userId(1).fullName("HR").build();
        requisition = JobRequisition.builder().requisitionId(101).requisitionCode("REQ-0101").title("Key Account Manager")
                .approvalStatus(RequisitionAccess.STATUS_APPROVED)
                .department(Department.builder().departmentId(2).departmentName("Sales").build())
                .minSalary(new BigDecimal("20000000")).maxSalary(new BigDecimal("30000000"))
                .numberOfPositions(2).screeningCriteria(new ArrayList<>()).build();
        lenient().when(postings.save(any(JobPosting.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ----- Hàm hỗ trợ -----

    /** Form hợp lệ: lương thỏa thuận, không hiện số lượng, hạn nộp 30 ngày sau, đăng ngay. */
    private static JobPostingCreateRequest validForm(String action) {
        return JobPostingCreateRequest.builder()
                .requisitionId(101)
                .postingTitle("Key Account Manager")
                .jobDescription("Chăm sóc khách hàng")
                .jobRequirements("2 năm kinh nghiệm")
                .salaryMode(JobPostingCreateRequest.SALARY_NEGOTIABLE)
                .showPositions(false)
                .applicationDeadline(TODAY.plusDays(30))
                .publishMode(JobPostingCreateRequest.PUBLISH_NOW)
                .action(action)
                .build();
    }

    /** Tin có sẵn trong DB với nội dung GIỐNG validForm (sửa mà không đổi gì thì không có thay đổi). */
    private JobPosting existingPosting(int id, String status, LocalDateTime postingDate) {
        JobPosting posting = JobPosting.builder().jobPostingId(id).postingNo(1).requisition(requisition)
                .postingTitle("Key Account Manager").jobDescription("Chăm sóc khách hàng")
                .jobRequirements("2 năm kinh nghiệm").salaryDisplay(JobPostingLabels.SALARY_NEGOTIABLE)
                .applicationDeadline(TODAY.plusDays(30).atTime(23, 59, 59))
                .postingStatus(status).postingDate(postingDate).createdBy(hr).build();
        lenient().when(postings.findForUpdate(id)).thenReturn(Optional.of(posting));
        lenient().when(postings.findById(id)).thenReturn(Optional.of(posting));
        return posting;
    }

    private JobPostingCreateRequest editForm(int id, String action) {
        JobPostingCreateRequest form = validForm(action);
        form.setJobPostingId(id);
        return form;
    }

    private void requisitionCanBeLocked() {
        when(requisitions.findForUpdate(101)).thenReturn(Optional.of(requisition));
        lenient().when(postings.existsByRequisition_RequisitionIdAndPostingStatusIn(eq(101), any())).thenReturn(false);
    }

    private JobPosting savedPosting() {
        ArgumentCaptor<JobPosting> captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(postings).save(captor.capture());
        return captor.getValue();
    }

    private AuditLog savedAudit() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(captor.capture());
        return captor.getValue();
    }

    // =========================================================================
    // Tạo tin
    // =========================================================================
    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Lưu nháp, lương 'Theo yêu cầu' -> Draft, không ngày đăng, lương lấy từ DB, tắt số lượng -> NULL, có audit")
        void createJobPosting_shouldSaveDraft_withSalaryFromRequisition() {
            // Arrange
            requisitionCanBeLocked();
            when(postings.findMaxPostingNo(101)).thenReturn(1);
            JobPostingCreateRequest form = validForm(JobPostingValidator.ACTION_DRAFT);
            form.setSalaryMode(JobPostingCreateRequest.SALARY_RANGE);
            form.setNumberOfPositionsDisplay("99 người");

            // Act
            service.createJobPosting(form, hr);

            // Assert
            JobPosting saved = savedPosting();
            assertEquals(JobPostingAccess.STATUS_DRAFT, saved.getPostingStatus());
            assertNull(saved.getPostingDate(), "Tin nháp không có ngày đăng");
            assertEquals("20 – 30 triệu VND/tháng", saved.getSalaryDisplay());
            assertNull(saved.getNumberOfPositionsDisplay());
            assertEquals(TODAY.plusDays(30).atTime(23, 59, 59), saved.getApplicationDeadline());
            assertEquals(2, saved.getPostingNo());
            assertEquals(hr, saved.getCreatedBy());
            assertEquals(JobPostingAuditLogger.ACTION_CREATE, savedAudit().getAction());
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("Đăng ngay -> Published, ngày đăng = giờ server (Clock), thông báo HM")
        void createAndPublishJobPosting_shouldPublishNow() {
            requisitionCanBeLocked();

            service.createAndPublishJobPosting(validForm(JobPostingValidator.ACTION_PUBLISH), hr);

            JobPosting saved = savedPosting();
            assertEquals(JobPostingAccess.STATUS_PUBLISHED, saved.getPostingStatus());
            assertEquals(NOW, saved.getPostingDate());
            verify(notificationService).notifyJobPostingPublished(saved, hr);
        }

        @Test
        @DisplayName("Hẹn đăng -> Published + PostingDate = thời điểm hẹn, ứng viên chưa thấy, có thông báo")
        void createAndPublishJobPosting_shouldSchedule_whenPublishLater() {
            // Arrange
            requisitionCanBeLocked();
            LocalDateTime at = TODAY.plusDays(3).atTime(8, 0);
            JobPostingCreateRequest form = validForm(JobPostingValidator.ACTION_PUBLISH);
            form.setPublishMode(JobPostingCreateRequest.PUBLISH_LATER);
            form.setScheduledAt(at);

            // Act
            service.createAndPublishJobPosting(form, hr);

            // Assert
            JobPosting saved = savedPosting();
            assertEquals(at, saved.getPostingDate());
            assertFalse(saved.isVisibleToCandidates(NOW), "Chưa tới giờ đăng thì ứng viên chưa thấy");
            verify(notificationService).notifyJobPostingPublished(saved, hr);
        }

        @Test
        @DisplayName("Không phải HR / Admin -> 403, không tra yêu cầu, không lưu, không audit")
        void createJobPosting_shouldDeny_whenViewerCannotManage() {
            doThrow(new AccessDeniedException("x")).when(access).requireManage(hr);

            assertThrows(AccessDeniedException.class,
                    () -> service.createJobPosting(validForm(JobPostingValidator.ACTION_DRAFT), hr));
            verifyNoInteractions(requisitions, audit, notificationService);
            verify(postings, never()).save(any());
        }

        @Test
        @DisplayName("Yêu cầu nguồn đã bị trả lại -> Lỗi, không lưu")
        void createJobPosting_shouldReject_whenRequisitionIsNotApproved() {
            requisition.setApprovalStatus(RequisitionAccess.STATUS_REJECTED);
            when(requisitions.findForUpdate(101)).thenReturn(Optional.of(requisition));

            assertThrows(JobPostingValidationException.class,
                    () -> service.createJobPosting(validForm(JobPostingValidator.ACTION_DRAFT), hr));
            verify(postings, never()).save(any());
        }

        @Test
        @DisplayName("Yêu cầu đã có tin đang mở -> Lỗi, không lưu (mỗi yêu cầu 1 tin đang mở)")
        void createJobPosting_shouldReject_whenRequisitionHasOpenPosting() {
            when(requisitions.findForUpdate(101)).thenReturn(Optional.of(requisition));
            when(postings.existsByRequisition_RequisitionIdAndPostingStatusIn(101, JobPostingAccess.OPEN_STATUSES))
                    .thenReturn(true);

            assertThrows(JobPostingValidationException.class,
                    () -> service.createJobPosting(validForm(JobPostingValidator.ACTION_DRAFT), hr));
            verify(postings, never()).save(any());
        }

        @Test
        @DisplayName("Đăng với hạn nộp hôm qua -> Lỗi theo ô, không lưu, không audit, không thông báo")
        void createAndPublishJobPosting_shouldReject_whenDeadlineIsInPast() {
            requisitionCanBeLocked();
            JobPostingCreateRequest form = validForm(JobPostingValidator.ACTION_PUBLISH);
            form.setApplicationDeadline(TODAY.minusDays(1));

            JobPostingValidationException exception = assertThrows(JobPostingValidationException.class,
                    () -> service.createAndPublishJobPosting(form, hr));
            assertTrue(exception.getFieldErrors().containsKey("applicationDeadline"));
            verify(postings, never()).save(any());
            verifyNoInteractions(audit, notificationService);
        }

        @Test
        @DisplayName("Form tạo bị sửa có jobPostingId (ID giả) -> Lỗi, không khóa yêu cầu")
        void createJobPosting_shouldReject_whenFormCarriesPostingId() {
            JobPostingCreateRequest form = validForm(JobPostingValidator.ACTION_DRAFT);
            form.setJobPostingId(999);

            assertThrows(JobPostingValidationException.class, () -> service.createJobPosting(form, hr));
            verifyNoInteractions(requisitions);
        }
    }

    // =========================================================================
    // Sửa tin
    // =========================================================================
    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Sửa tiêu đề tin đang đăng -> Giữ ngày đăng, audit 'Tiêu đề tin: cũ → mới', không thông báo lại")
        void updateJobPosting_shouldKeepPostingDate_whenPostingIsLive() {
            // Arrange
            LocalDateTime postedAt = NOW.minusDays(5);
            JobPosting live = existingPosting(9, JobPostingAccess.STATUS_PUBLISHED, postedAt);
            JobPostingCreateRequest form = editForm(9, JobPostingValidator.ACTION_DRAFT);
            form.setPostingTitle("Key Account Manager (B2B)");

            // Act
            service.updateJobPosting(9, form, hr);

            // Assert
            assertEquals(JobPostingAccess.STATUS_PUBLISHED, live.getPostingStatus());
            assertEquals(postedAt, live.getPostingDate());
            AuditLog log = savedAudit();
            assertEquals(JobPostingAuditLogger.ACTION_UPDATE, log.getAction());
            assertTrue(log.getNewValue().startsWith(JobPostingLabels.LOG_UPDATE));
            assertTrue(log.getNewValue().contains("Tiêu đề tin: Key Account Manager → Key Account Manager (B2B)"));
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("Bấm 'Lưu thay đổi' mà không đổi gì -> Không lưu, không ghi audit")
        void updateJobPosting_shouldSkipSaveAndAudit_whenNothingChanged() {
            existingPosting(9, JobPostingAccess.STATUS_PUBLISHED, NOW.minusDays(5));

            service.updateJobPosting(9, editForm(9, JobPostingValidator.ACTION_DRAFT), hr);

            verify(postings, never()).save(any());
            verifyNoInteractions(audit, notificationService);
        }

        @Test
        @DisplayName("Tin hẹn đăng, đổi sang 'Đăng ngay' rồi lưu -> Đăng luôn (ngày đăng = bây giờ), có thông báo")
        void updateJobPosting_shouldPublishNow_whenScheduledPostingSwitchesToNow() {
            JobPosting scheduled = existingPosting(10, JobPostingAccess.STATUS_PUBLISHED, NOW.plusDays(5));

            service.updateJobPosting(10, editForm(10, JobPostingValidator.ACTION_DRAFT), hr);

            assertEquals(NOW, scheduled.getPostingDate());
            assertTrue(savedAudit().getNewValue().startsWith(JobPostingLabels.LOG_PUBLISH));
            verify(notificationService).notifyJobPostingPublished(scheduled, hr);
        }

        @Test
        @DisplayName("Tin tạm dừng: 'Lưu thay đổi' vẫn tạm dừng; 'Lưu và mở lại' -> Published, ngày đăng = bây giờ")
        void updateJobPosting_shouldKeepPausedOrReopen_dependingOnAction() {
            // Arrange
            JobPosting paused = existingPosting(11, JobPostingAccess.STATUS_PAUSED, NOW.minusDays(10));
            JobPostingCreateRequest form = editForm(11, JobPostingValidator.ACTION_DRAFT);
            form.setBenefits("- Lương tháng 13");

            // Act + Assert: lưu thay đổi
            service.updateJobPosting(11, form, hr);
            assertEquals(JobPostingAccess.STATUS_PAUSED, paused.getPostingStatus());

            // Act + Assert: lưu và mở lại
            form.setAction(JobPostingValidator.ACTION_PUBLISH);
            service.updateAndPublishJobPosting(11, form, hr);
            assertEquals(JobPostingAccess.STATUS_PUBLISHED, paused.getPostingStatus());
            assertEquals(NOW, paused.getPostingDate());
            verify(notificationService).notifyJobPostingPublished(paused, hr);
        }

        @Test
        @DisplayName("Sửa tin đã đóng -> Lỗi, nội dung và trạng thái giữ nguyên, không lưu")
        void updateJobPosting_shouldReject_whenPostingIsClosed() {
            JobPosting closed = existingPosting(12, JobPostingAccess.STATUS_CLOSED, NOW.minusDays(40));
            JobPostingCreateRequest form = editForm(12, JobPostingValidator.ACTION_DRAFT);
            form.setPostingTitle("Đổi tiêu đề");

            assertThrows(JobPostingValidationException.class, () -> service.updateJobPosting(12, form, hr));
            assertEquals(JobPostingAccess.STATUS_CLOSED, closed.getPostingStatus());
            assertEquals("Key Account Manager", closed.getPostingTitle());
            verify(postings, never()).save(any());
            verifyNoInteractions(audit);
        }

        @Test
        @DisplayName("Form sửa gửi requisitionId của yêu cầu khác (ID giả) -> Lỗi, không lưu")
        void updateJobPosting_shouldReject_whenRequisitionIdIsTampered() {
            existingPosting(13, JobPostingAccess.STATUS_DRAFT, null);
            JobPostingCreateRequest form = editForm(13, JobPostingValidator.ACTION_DRAFT);
            form.setRequisitionId(999);

            assertThrows(JobPostingValidationException.class, () -> service.updateJobPosting(13, form, hr));
            verify(postings, never()).save(any());
        }

        @Test
        @DisplayName("Id tin không tồn tại -> ResourceNotFoundException (404)")
        void updateJobPosting_shouldThrowNotFound_whenPostingDoesNotExist() {
            when(postings.findForUpdate(404)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.updateJobPosting(404, editForm(404, JobPostingValidator.ACTION_DRAFT), hr));
        }
    }

    // =========================================================================
    // Chuyển trạng thái
    // =========================================================================
    @Nested
    @DisplayName("Chuyển trạng thái")
    class Transitions {

        @Test
        @DisplayName("Đăng tin nháp -> Published, ngày đăng = bây giờ, audit + thông báo")
        void publishJobPosting_shouldPublishDraft() {
            JobPosting draft = existingPosting(20, JobPostingAccess.STATUS_DRAFT, null);
            when(postings.existsByRequisition_RequisitionIdAndJobPostingIdNotAndPostingStatusIn(eq(101), eq(20), any()))
                    .thenReturn(false);

            service.publishJobPosting(20, hr);

            assertEquals(JobPostingAccess.STATUS_PUBLISHED, draft.getPostingStatus());
            assertEquals(NOW, draft.getPostingDate());
            assertTrue(savedAudit().getNewValue().startsWith(JobPostingLabels.LOG_PUBLISH));
            verify(notificationService).notifyJobPostingPublished(draft, hr);
        }

        @Test
        @DisplayName("Đăng tin đang đăng -> Lỗi, trạng thái giữ nguyên, không thông báo")
        void publishJobPosting_shouldReject_whenAlreadyPublished() {
            JobPosting published = existingPosting(21, JobPostingAccess.STATUS_PUBLISHED, NOW.minusDays(1));

            assertThrows(JobPostingValidationException.class, () -> service.publishJobPosting(21, hr));
            assertEquals(NOW.minusDays(1), published.getPostingDate());
            verify(postings, never()).save(any());
            verifyNoInteractions(notificationService, audit);
        }

        @Test
        @DisplayName("Đăng tin nháp đã quá hạn nộp -> Lỗi 'Hạn nộp đã qua', vẫn là nháp")
        void publishJobPosting_shouldReject_whenDeadlinePassed() {
            JobPosting draft = existingPosting(22, JobPostingAccess.STATUS_DRAFT, null);
            draft.setApplicationDeadline(NOW.minusHours(1));

            assertThrows(JobPostingValidationException.class, () -> service.publishJobPosting(22, hr));
            assertEquals(JobPostingAccess.STATUS_DRAFT, draft.getPostingStatus());
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("Đăng ngay tin hẹn đăng -> Ngày đăng = bây giờ, có thông báo; tin không hẹn đăng -> Lỗi")
        void publishNow_shouldMovePostingDateToNow_onlyForScheduledPosting() {
            JobPosting scheduled = existingPosting(23, JobPostingAccess.STATUS_PUBLISHED, NOW.plusDays(2));

            service.publishNow(23, hr);

            assertEquals(NOW, scheduled.getPostingDate());
            verify(notificationService).notifyJobPostingPublished(scheduled, hr);

            existingPosting(24, JobPostingAccess.STATUS_PUBLISHED, NOW.minusDays(1));
            assertThrows(JobPostingValidationException.class, () -> service.publishNow(24, hr));
        }

        @Test
        @DisplayName("Hủy lịch đăng -> Về bản nháp, PostingDate = NULL, không thông báo")
        void unschedule_shouldReturnToDraft() {
            JobPosting scheduled = existingPosting(25, JobPostingAccess.STATUS_PUBLISHED, NOW.plusDays(2));

            service.unschedule(25, hr);

            assertEquals(JobPostingAccess.STATUS_DRAFT, scheduled.getPostingStatus());
            assertNull(scheduled.getPostingDate());
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("Tạm dừng tin đang đăng -> Paused; tạm dừng bản nháp -> Lỗi, vẫn là nháp")
        void pauseJobPosting_shouldOnlyPausePublishedPosting() {
            JobPosting published = existingPosting(26, JobPostingAccess.STATUS_PUBLISHED, NOW.minusDays(1));
            service.pauseJobPosting(26, hr);
            assertEquals(JobPostingAccess.STATUS_PAUSED, published.getPostingStatus());

            JobPosting draft = existingPosting(27, JobPostingAccess.STATUS_DRAFT, null);
            assertThrows(JobPostingValidationException.class, () -> service.pauseJobPosting(27, hr));
            assertEquals(JobPostingAccess.STATUS_DRAFT, draft.getPostingStatus());
        }

        @Test
        @DisplayName("Admin đóng tin (D7: chỉ HR) -> 403, không gọi talent pool")
        void closeJobPosting_shouldDeny_whenViewerIsNotHr() {
            doThrow(new AccessDeniedException("x")).when(access).requireCloseAny(hr);

            assertThrows(AccessDeniedException.class, () -> service.closeJobPosting(28, hr));
            verifyNoInteractions(talentPoolService);
        }

        @Test
        @DisplayName("HR đóng tin -> Giao cho TalentPoolService#closeJob")
        void closeJobPosting_shouldDelegateToTalentPool_whenViewerIsHr() {
            service.closeJobPosting(28, hr);

            verify(talentPoolService).closeJob(28);
        }

        @Test
        @DisplayName("Xóa bản nháp chưa có hồ sơ -> Ghi audit TRƯỚC rồi mới xóa")
        void deleteJobPosting_shouldAuditBeforeDelete() {
            JobPosting draft = existingPosting(29, JobPostingAccess.STATUS_DRAFT, null);
            when(applications.existsByJobPosting_JobPostingId(29)).thenReturn(false);

            service.deleteJobPosting(29, hr);

            InOrder order = inOrder(audit, postings);
            order.verify(audit).save(any(AuditLog.class));
            order.verify(postings).delete(draft);
        }

        @Test
        @DisplayName("Xóa bản nháp đã có hồ sơ / tin đang đăng -> Lỗi, không xóa, không audit")
        void deleteJobPosting_shouldReject_whenDraftHasApplicationsOrNotDraft() {
            existingPosting(30, JobPostingAccess.STATUS_DRAFT, null);
            when(applications.existsByJobPosting_JobPostingId(30)).thenReturn(true);
            assertThrows(JobPostingValidationException.class, () -> service.deleteJobPosting(30, hr));

            existingPosting(31, JobPostingAccess.STATUS_PUBLISHED, NOW.minusDays(1));
            assertThrows(JobPostingValidationException.class, () -> service.deleteJobPosting(31, hr));

            verify(postings, never()).delete(any(JobPosting.class));
            verifyNoInteractions(audit);
        }
    }

    // =========================================================================
    // Trang chi tiết
    // =========================================================================
    @Nested
    @DisplayName("getInternalJobPostingDetail")
    class Detail {

        @Test
        @DisplayName("Tin đang đăng -> Số liệu đếm theo trạng thái, chờ xử lý = mới nộp + AI đã chấm; ngưỡng '70.00' hiện 70")
        void getInternalJobPostingDetail_shouldCountApplications_whenPostingIsLive() {
            // Arrange
            existingPosting(40, JobPostingAccess.STATUS_PUBLISHED, NOW.minusDays(10));
            when(postings.findConfigValue("AI_SCORE_THRESHOLD")).thenReturn(Optional.of("70.00"));
            when(postings.countApplicationsByStatus(40)).thenReturn(List.of(
                    new Object[] {"Applied", 4L}, new Object[] {"AI_Screened", 5L}, new Object[] {"Interviewing", 2L},
                    new Object[] {"Hired", 1L}, new Object[] {"Rejected", 2L}));
            when(postings.countApplicationsSince(40, NOW.minusDays(7))).thenReturn(2L);
            when(postings.countApplicationsWithAiScoreAtLeast(40, new BigDecimal("70"))).thenReturn(7L);
            when(postings.findRecentApplications(eq(40), any(Pageable.class))).thenReturn(List.of());
            when(postings.findByRequisition_RequisitionIdOrderByPostingNoAsc(101)).thenReturn(List.of());

            // Act
            InternalJobPostingDetailResponse detail = service.getInternalJobPostingDetail(40, hr);

            // Assert
            verify(access).requireView(eq(hr), any(JobPosting.class));
            assertEquals(14, detail.totalApplications());
            assertEquals(9, detail.pendingApplications());
            assertEquals(7, detail.aiPassedApplications());
            assertEquals(2, detail.applicationsLast7Days());
            assertEquals(1, detail.hiredApplications());
            assertEquals(2, detail.interviewingApplications());
            assertEquals("70", detail.aiThreshold().toPlainString());
        }

        @Test
        @DisplayName("Tin hẹn đăng -> Chưa có số liệu, không chạy câu đếm hồ sơ")
        void getInternalJobPostingDetail_shouldSkipStats_whenPostingIsScheduled() {
            existingPosting(41, JobPostingAccess.STATUS_PUBLISHED, NOW.plusDays(3));
            when(postings.findConfigValue(any())).thenReturn(Optional.empty());

            InternalJobPostingDetailResponse detail = service.getInternalJobPostingDetail(41, hr);

            assertEquals(0, detail.totalApplications());
            verify(postings, never()).countApplicationsByStatus(any());
        }

        @Test
        @DisplayName("Người xem không có quyền với tin (HM phòng khác / Director xem nháp) -> 403, không tra số liệu")
        void getInternalJobPostingDetail_shouldDeny_whenViewerCannotViewPosting() {
            JobPosting draft = existingPosting(42, JobPostingAccess.STATUS_DRAFT, null);
            doThrow(new AccessDeniedException("x")).when(access).requireView(hr, draft);

            assertThrows(AccessDeniedException.class, () -> service.getInternalJobPostingDetail(42, hr));
            verify(postings, never()).countApplicationsByStatus(any());
        }

        @Test
        @DisplayName("Các lần đăng: đếm hồ sơ và số đã tuyển gộp 2 câu cho mọi lần đăng")
        void getInternalJobPostingDetail_shouldCountRoundsInBatch() {
            // Arrange
            JobPosting current = existingPosting(43, JobPostingAccess.STATUS_CLOSED, NOW.minusDays(40));
            JobPosting firstRound = JobPosting.builder().jobPostingId(44).postingNo(2).postingStatus(JobPostingAccess.STATUS_CLOSED)
                    .requisition(requisition).build();
            when(postings.findConfigValue(any())).thenReturn(Optional.empty());
            when(postings.countApplicationsByStatus(43)).thenReturn(List.of());
            when(postings.findRecentApplications(eq(43), any(Pageable.class))).thenReturn(List.of());
            when(postings.findByRequisition_RequisitionIdOrderByPostingNoAsc(101))
                    .thenReturn(List.of(current, firstRound));
            when(postings.countApplicationsByPosting(anyCollection(), any())).thenReturn(List.<Object[]>of(new Object[] {44, 23L, 0L}));
            when(postings.countApplicationsInStatusByPosting(anyCollection(), eq("Hired"))).thenReturn(List.of());

            // Act
            InternalJobPostingDetailResponse detail = service.getInternalJobPostingDetail(43, hr);

            // Assert
            verify(postings, times(1)).countApplicationsByPosting(anyCollection(), any());
            verify(postings, times(1)).countApplicationsInStatusByPosting(anyCollection(), any());
            assertEquals(2, detail.rounds().size());
            assertTrue(detail.rounds().get(0).current());
            assertEquals(23, detail.rounds().get(1).applications());
        }
    }

    // =========================================================================
    // Mở form
    // =========================================================================
    @Nested
    @DisplayName("Mở form")
    class Forms {

        @Test
        @DisplayName("Mở form tạo từ yêu cầu đã duyệt, đã đăng 1 lần -> Tin lần 2, điền sẵn tiêu đề")
        void prepareCreateForm_shouldPrefill_whenRequisitionIsApproved() {
            when(requisitions.findById(101)).thenReturn(Optional.of(requisition));
            when(postings.findMaxPostingNo(101)).thenReturn(1);

            JobPostingCreateRequest form = service.prepareCreateForm(101, hr);

            assertEquals(2, form.getPostingNo());
            assertEquals("Key Account Manager", form.getPostingTitle());
        }

        @Test
        @DisplayName("Thiếu requisitionId -> Lỗi nghiệp vụ; id không tồn tại -> 404")
        void prepareCreateForm_shouldRejectMissingOrUnknownRequisition() {
            assertThrows(JobPostingValidationException.class, () -> service.prepareCreateForm(null, hr));

            when(requisitions.findById(404)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.prepareCreateForm(404, hr));
        }

        @Test
        @DisplayName("Đăng lại (D10): tin đã đóng -> Form tin lần mới, nội dung từ tin cũ; tin chưa đóng -> Lỗi")
        void prepareRepostForm_shouldCopyContent_onlyFromClosedPosting() {
            // Arrange
            JobPosting closed = existingPosting(50, JobPostingAccess.STATUS_CLOSED, NOW.minusDays(40));
            closed.setSalaryDisplay("Lên đến 45 triệu");
            closed.setBenefits("- Lương tháng 13");
            when(requisitions.findById(101)).thenReturn(Optional.of(requisition));
            when(postings.existsByRequisition_RequisitionIdAndPostingStatusIn(eq(101), any())).thenReturn(false);
            when(postings.findMaxPostingNo(101)).thenReturn(2);

            // Act
            JobPostingCreateRequest form = service.prepareRepostForm(50, hr);

            // Assert
            assertNull(form.getJobPostingId(), "Là tin mới");
            assertEquals(3, form.getPostingNo());
            assertEquals(JobPostingCreateRequest.SALARY_CUSTOM, form.getSalaryMode());
            assertEquals("- Lương tháng 13", form.getBenefits());

            existingPosting(51, JobPostingAccess.STATUS_PAUSED, NOW.minusDays(3));
            assertThrows(JobPostingValidationException.class, () -> service.prepareRepostForm(51, hr));
        }

        @Test
        @DisplayName("Mở form sửa tin đã đóng -> Lỗi")
        void prepareEditForm_shouldReject_whenPostingIsClosed() {
            existingPosting(52, JobPostingAccess.STATUS_CLOSED, NOW.minusDays(40));

            assertThrows(JobPostingValidationException.class, () -> service.prepareEditForm(52, hr));
        }
    }

    // =========================================================================
    // Bảng điều khiển + ô chọn yêu cầu
    // =========================================================================
    @Nested
    @DisplayName("Bảng điều khiển")
    class Dashboard {

        @Test
        @DisplayName("Ô 'Đổi yêu cầu': số lần đã đăng tra gộp 1 câu (không N+1), nhãn '(đã đăng 2 lần)'")
        void requisitionChoices_shouldBatchPostingCounts() {
            JobRequisition other = JobRequisition.builder().requisitionId(102).requisitionCode("REQ-0102").title("Tester").build();
            when(postings.findRequisitionIdsWithPostingStatusIn(JobPostingAccess.OPEN_STATUSES)).thenReturn(Set.of());
            when(requisitions.findWithDepartmentByApprovalStatusOrderByCreatedAtDesc(RequisitionAccess.STATUS_APPROVED))
                    .thenReturn(List.of(requisition, other));
            when(postings.findMaxPostingNoByRequisitionIds(anyCollection())).thenReturn(List.<Object[]>of(new Object[] {101, 2}));

            List<FilterOption> choices = service.requisitionChoices(hr);

            assertEquals("Key Account Manager · REQ-0101 (đã đăng 2 lần)", choices.get(0).label());
            assertEquals("Tester · REQ-0102", choices.get(1).label());
            verify(postings, times(1)).findMaxPostingNoByRequisitionIds(anyCollection());
            verify(postings, never()).findMaxPostingNo(anyInt());
        }

        @Test
        @DisplayName("Bảng điều khiển -> Đếm bằng SQL (không tải mọi tin), tỷ lệ phòng ban theo tổng số tin")
        void getDashboard_shouldUseCountQueries() {
            // Arrange
            when(postings.countWithOpenDeadline(JobPostingAccess.STATUS_PUBLISHED, NOW)).thenReturn(3L);
            when(postings.countByPostingStatus(JobPostingAccess.STATUS_DRAFT)).thenReturn(2L);
            when(postings.countByPostingStatus(JobPostingAccess.STATUS_CLOSED)).thenReturn(4L);
            when(postings.countWithPassedDeadline(JobPostingAccess.STATUS_PUBLISHED, NOW)).thenReturn(1L);
            when(postings.count()).thenReturn(10L);
            when(postings.countByDepartment()).thenReturn(List.<Object[]>of(new Object[] {"Sales", 4L}));
            when(postings.findRecent(any(Pageable.class))).thenReturn(List.of());
            when(postings.findRequisitionIdsWithPostingStatusIn(any())).thenReturn(Set.of(101));
            when(requisitions.findWithDepartmentByApprovalStatusOrderByCreatedAtDesc(any())).thenReturn(List.of(requisition));

            // Act
            JobPostingDashboardResponse dashboard = service.getDashboard(hr);

            // Assert
            assertEquals(3, dashboard.activeCount());
            assertEquals(2, dashboard.draftCount());
            assertEquals(5, dashboard.closedCount());
            assertEquals(40, dashboard.departmentStats().get(0).percentage());
            assertTrue(dashboard.requisitionsAwaitingPosting().isEmpty(), "Yêu cầu đã có tin đang mở thì không chờ đăng");
            verify(postings, never()).findAll();
        }
    }
}
