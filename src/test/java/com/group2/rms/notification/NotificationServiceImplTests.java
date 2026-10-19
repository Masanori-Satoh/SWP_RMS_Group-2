package com.group2.rms.notification;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/*
 * UNIT TEST SERVICE: NotificationServiceImpl (tạo thông báo cho từng sự kiện, đọc danh sách, đánh dấu đã đọc).
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTests {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZONE);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    private NotificationRepository notifications;

    private NotificationServiceImpl service;

    private User manager;
    private User director;
    private User hr;
    private JobRequisition requisition;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(notifications, CLOCK);
        manager = User.builder().userId(10).username("hm").fullName("Trần Thị Quản Lý").build();
        director = User.builder().userId(20).username("director").fullName("Nguyễn Văn Giám Đốc").build();
        hr = User.builder().userId(30).username("hr").fullName("Lê Thị Nhân Sự").build();
        requisition = JobRequisition.builder().requisitionId(101).requisitionCode("REQ-0101")
                .title("Senior Java Engineer").hiringManager(manager).build();
    }

    // ----- Hàm hỗ trợ -----

    private RequisitionApproval rejection(int approvalId, String comment) {
        return RequisitionApproval.builder().approvalId(approvalId).requisition(requisition).director(director)
                .status("Rejected").comments(comment).approvalDate(LocalDateTime.of(2026, 10, 4, 9, 30)).build();
    }

    private JobPosting posting(LocalDateTime postingDate) {
        return JobPosting.builder().jobPostingId(50).postingTitle("Senior Java Engineer").requisition(requisition)
                .postingDate(postingDate).applicationDeadline(LocalDateTime.of(2026, 11, 30, 23, 59, 59)).build();
    }

    /** Thư mời của một ứng viên có tài khoản; {@code proposedBy} là HR gửi thư (null = không rõ HR). */
    private static OfferProposal offer(User candidateAccount, User proposedBy, String positionTitle) {
        Candidate candidate = Candidate.builder().candidateId(5).account(candidateAccount).build();
        Application application = Application.builder().applicationId(10).candidate(candidate).build();
        return OfferProposal.builder().offerId(1).offeredPositionTitle(positionTitle).proposedBy(proposedBy)
                .application(application).build();
    }

    private static User candidateAccount() {
        return User.builder().userId(22).username("trung").fullName("Huỳnh Hoàng Trung").build();
    }

    /** Thông báo duy nhất đã được lưu. */
    private Notification savedNotification() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(captor.capture());
        return captor.getValue();
    }

    // =========================================================================
    // notifyRequisitionRejected
    // =========================================================================

    @Nested
    @DisplayName("notifyRequisitionRejected")
    class NotifyRequisitionRejected {

        @Test
        @DisplayName("Giám đốc từ chối có lý do -> Lưu 1 thông báo cho đúng HM, có lý do, người từ chối, giờ và link chi tiết")
        void notifyRequisitionRejected_shouldSaveForHiringManager_whenRejectionIsValid() {
            // Act
            service.notifyRequisitionRejected(rejection(7, "Kinh phí dự kiến vượt trần"));

            // Assert
            Notification saved = savedNotification();
            assertEquals(manager, saved.getRecipient());
            assertEquals(director, saved.getActor());
            assertEquals("REQUISITION_REJECTED", saved.getEventType());
            assertEquals("101", saved.getReferenceId());
            assertEquals("/requisitions/101", saved.getLinkUrl());
            assertEquals("REQ_REJECT_7", saved.getEventId());
            assertFalse(saved.getIsRead());
            assertEquals("Yêu cầu tuyển dụng đã bị từ chối: Senior Java Engineer", saved.getTitle());
            assertTrue(saved.getContent().contains("REQ-0101"));
            assertTrue(saved.getContent().contains("đã bị Giám đốc Nguyễn Văn Giám Đốc từ chối vào lúc 04/10/2026 09:30"));
            assertTrue(saved.getContent().contains("Lý do: Kinh phí dự kiến vượt trần."));
        }

        @Test
        @DisplayName("Lý do có khoảng trắng đầu / cuối -> Được cắt trước khi đưa vào nội dung")
        void notifyRequisitionRejected_shouldTrimReason_whenReasonHasSurroundingSpaces() {
            // Act
            service.notifyRequisitionRejected(rejection(7, "   Thiếu mô tả công việc   "));

            // Assert
            assertTrue(savedNotification().getContent().contains("Lý do: Thiếu mô tả công việc."));
        }

        @ParameterizedTest(name = "lý do = [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Lý do null / rỗng / chỉ khoảng trắng -> Nội dung ghi 'Không có lý do cụ thể'")
        void notifyRequisitionRejected_shouldUseDefaultReason_whenReasonIsBlank(String reason) {
            // Act
            service.notifyRequisitionRejected(rejection(7, reason));

            // Assert
            assertTrue(savedNotification().getContent().contains("Lý do: Không có lý do cụ thể."));
        }

        @Test
        @DisplayName("Không có người từ chối và ngày quyết định -> Ghi 'Giám đốc' (không lặp chữ) và lấy giờ hiện tại")
        void notifyRequisitionRejected_shouldUseDefaults_whenDirectorAndDateAreMissing() {
            // Arrange
            RequisitionApproval rejection = rejection(7, "Lý do");
            rejection.setDirector(null);
            rejection.setApprovalDate(null);

            // Act
            service.notifyRequisitionRejected(rejection);

            // Assert
            Notification saved = savedNotification();
            assertNull(saved.getActor());
            assertTrue(saved.getContent().contains("đã bị Giám đốc từ chối vào lúc 18/10/2026 10:00"));
            assertFalse(saved.getContent().contains("Giám đốc Giám đốc"));
        }

        @Test
        @DisplayName("Yêu cầu chưa có mã -> Dùng 'REQ-{id}' trong nội dung")
        void notifyRequisitionRejected_shouldFallBackToIdCode_whenRequisitionCodeIsMissing() {
            // Arrange
            requisition.setRequisitionCode(null);

            // Act
            service.notifyRequisitionRejected(rejection(7, "Lý do"));

            // Assert
            assertTrue(savedNotification().getContent().contains("(Mã: REQ-101)"));
        }

        @Test
        @DisplayName("Bản ghi từ chối null -> Không lưu, không gọi repository")
        void notifyRequisitionRejected_shouldDoNothing_whenRejectionIsNull() {
            // Act
            service.notifyRequisitionRejected(null);

            // Assert
            verifyNoInteractions(notifications);
        }

        @Test
        @DisplayName("Yêu cầu không có HM -> Không lưu (không biết gửi cho ai)")
        void notifyRequisitionRejected_shouldDoNothing_whenRequisitionHasNoHiringManager() {
            // Arrange
            requisition.setHiringManager(null);

            // Act
            service.notifyRequisitionRejected(rejection(7, "Lý do"));

            // Assert
            verifyNoInteractions(notifications);
        }

        @Test
        @DisplayName("Cùng lần từ chối gửi lại (eventId đã có) -> Không tạo thông báo trùng")
        void notifyRequisitionRejected_shouldSkip_whenEventIdAlreadyExists() {
            // Arrange
            when(notifications.existsByRecipient_UserIdAndEventId(10, "REQ_REJECT_7")).thenReturn(true);

            // Act
            service.notifyRequisitionRejected(rejection(7, "Lý do"));

            // Assert
            verify(notifications, never()).save(any());
        }

        @Test
        @DisplayName("Từ chối lần 2 sau khi HM nộp lại -> Tạo thông báo mới với eventId khác, giữ thông báo cũ")
        void notifyRequisitionRejected_shouldCreateNewNotification_whenRejectedAgainAfterResubmit() {
            // Act
            service.notifyRequisitionRejected(rejection(7, "Lần 1"));
            service.notifyRequisitionRejected(rejection(8, "Lần 2"));

            // Assert
            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notifications, times(2)).save(captor.capture());
            assertEquals("REQ_REJECT_7", captor.getAllValues().get(0).getEventId());
            assertEquals("REQ_REJECT_8", captor.getAllValues().get(1).getEventId());
        }
    }

    // =========================================================================
    // notifyRequisitionApproved
    // =========================================================================

    @Nested
    @DisplayName("notifyRequisitionApproved")
    class NotifyRequisitionApproved {

        private RequisitionApproval approval(int approvalId, String comment) {
            return RequisitionApproval.builder().approvalId(approvalId).requisition(requisition).director(director)
                    .status("Approved").comments(comment).approvalDate(LocalDateTime.of(2026, 10, 4, 9, 30)).build();
        }

        @Test
        @DisplayName("Giám đốc duyệt có ý kiến -> Lưu 1 thông báo cho đúng HM, có ý kiến, người duyệt, giờ và link chi tiết")
        void notifyRequisitionApproved_shouldSaveForHiringManager_whenApprovalIsValid() {
            // Act
            service.notifyRequisitionApproved(approval(9, "  Ưu tiên tuyển trước tháng 12  "));

            // Assert
            Notification saved = savedNotification();
            assertEquals(manager, saved.getRecipient());
            assertEquals(director, saved.getActor());
            assertEquals("REQUISITION_APPROVED", saved.getEventType());
            assertEquals("101", saved.getReferenceId());
            assertEquals("/requisitions/101", saved.getLinkUrl());
            assertEquals("REQ_APPROVE_9", saved.getEventId());
            assertFalse(saved.getIsRead());
            assertEquals("Yêu cầu tuyển dụng đã được phê duyệt: Senior Java Engineer", saved.getTitle());
            assertTrue(saved.getContent().contains("(Mã: REQ-0101)"));
            assertTrue(saved.getContent().contains("đã được Giám đốc Nguyễn Văn Giám Đốc phê duyệt vào lúc 04/10/2026 09:30"));
            assertTrue(saved.getContent().contains("Ý kiến: Ưu tiên tuyển trước tháng 12."));
            assertTrue(saved.getContent().contains("HR có thể tạo tin tuyển dụng cho yêu cầu này."));
        }

        @ParameterizedTest(name = "ý kiến = [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Duyệt không ghi ý kiến -> Nội dung không có dòng 'Ý kiến'")
        void notifyRequisitionApproved_shouldOmitComment_whenCommentIsBlank(String comment) {
            // Act
            service.notifyRequisitionApproved(approval(9, comment));

            // Assert
            assertFalse(savedNotification().getContent().contains("Ý kiến"));
        }

        @Test
        @DisplayName("Bản ghi duyệt null -> Không lưu, không gọi repository")
        void notifyRequisitionApproved_shouldDoNothing_whenApprovalIsNull() {
            // Act
            service.notifyRequisitionApproved(null);

            // Assert
            verifyNoInteractions(notifications);
        }

        @Test
        @DisplayName("Yêu cầu không có HM -> Không lưu (không biết gửi cho ai)")
        void notifyRequisitionApproved_shouldDoNothing_whenRequisitionHasNoHiringManager() {
            // Arrange
            requisition.setHiringManager(null);

            // Act
            service.notifyRequisitionApproved(approval(9, null));

            // Assert
            verifyNoInteractions(notifications);
        }

        @Test
        @DisplayName("Cùng lần duyệt gửi lại (eventId đã có) -> Không tạo thông báo trùng")
        void notifyRequisitionApproved_shouldSkip_whenEventIdAlreadyExists() {
            // Arrange
            when(notifications.existsByRecipient_UserIdAndEventId(10, "REQ_APPROVE_9")).thenReturn(true);

            // Act
            service.notifyRequisitionApproved(approval(9, null));

            // Assert
            verify(notifications, never()).save(any());
        }
    }

    // =========================================================================
    // notifyJobPostingPublished
    // =========================================================================

    @Nested
    @DisplayName("notifyJobPostingPublished")
    class NotifyJobPostingPublished {

        @Test
        @DisplayName("Đăng ngay (ngày đăng trước hiện tại) -> Báo 'đã phát hành' cho HM, có HR, hạn nộp, link tin nội bộ")
        void notifyJobPostingPublished_shouldSayPublished_whenPostingDateIsInThePast() {
            // Arrange
            LocalDateTime postedAt = LocalDateTime.of(2026, 10, 5, 14, 0, 0, 123456700);

            // Act
            service.notifyJobPostingPublished(posting(postedAt), hr);

            // Assert
            Notification saved = savedNotification();
            assertEquals(manager, saved.getRecipient());
            assertEquals(hr, saved.getActor());
            assertEquals("JOB_POSTING_PUBLISHED", saved.getEventType());
            assertEquals("50", saved.getReferenceId());
            assertEquals("/internal/job-postings/50", saved.getLinkUrl());
            assertEquals("JOB_PUB_50_202610051400001234567", saved.getEventId());
            assertEquals("Tin tuyển dụng đã phát hành: Senior Java Engineer", saved.getTitle());
            assertTrue(saved.getContent().contains("phát hành lên cổng việc làm công khai vào lúc 05/10/2026 14:00"));
            assertTrue(saved.getContent().contains("Lê Thị Nhân Sự"));
            assertTrue(saved.getContent().contains("REQ-0101"));
            assertTrue(saved.getContent().contains("Hạn nộp hồ sơ: 30/11/2026"));
        }

        @Test
        @DisplayName("Hẹn đăng (ngày đăng sau hiện tại) -> Báo 'hẹn đăng', ghi rõ ứng viên chưa thấy tin")
        void notifyJobPostingPublished_shouldSayScheduled_whenPostingDateIsInTheFuture() {
            // Act
            service.notifyJobPostingPublished(posting(NOW.plusDays(3)), hr);

            // Assert
            Notification saved = savedNotification();
            assertEquals("Tin tuyển dụng hẹn đăng: Senior Java Engineer", saved.getTitle());
            assertTrue(saved.getContent().contains("hẹn đăng lên cổng việc làm công khai vào lúc 21/10/2026 10:00"));
            assertTrue(saved.getContent().contains("Trước thời điểm đó ứng viên chưa thấy tin."));
        }

        @Test
        @DisplayName("Ngày đăng đúng bằng hiện tại (biên) -> Tính là đăng ngay, không phải hẹn đăng")
        void notifyJobPostingPublished_shouldSayPublished_whenPostingDateEqualsNow() {
            // Act
            service.notifyJobPostingPublished(posting(NOW), hr);

            // Assert
            assertEquals("Tin tuyển dụng đã phát hành: Senior Java Engineer", savedNotification().getTitle());
        }

        @Test
        @DisplayName("Chưa có ngày đăng -> Lấy giờ hiện tại, báo 'đã phát hành'")
        void notifyJobPostingPublished_shouldUseNow_whenPostingDateIsMissing() {
            // Act
            service.notifyJobPostingPublished(posting(null), hr);

            // Assert
            Notification saved = savedNotification();
            assertTrue(saved.getTitle().startsWith("Tin tuyển dụng đã phát hành"));
            assertTrue(saved.getContent().contains("18/10/2026 10:00"));
        }

        @Test
        @DisplayName("Không có hạn nộp và không rõ HR -> Ghi 'Không giới hạn' và 'Nhân sự (HR)'")
        void notifyJobPostingPublished_shouldUseDefaults_whenDeadlineAndHrAreMissing() {
            // Arrange
            JobPosting posting = posting(NOW.minusDays(1));
            posting.setApplicationDeadline(null);

            // Act
            service.notifyJobPostingPublished(posting, null);

            // Assert
            Notification saved = savedNotification();
            assertNull(saved.getActor());
            assertTrue(saved.getContent().contains("Hạn nộp hồ sơ: Không giới hạn."));
            assertTrue(saved.getContent().contains("nhân sự Nhân sự (HR)"));
        }

        @Test
        @DisplayName("Đổi lịch đăng / mở lại (ngày đăng khác) -> eventId khác nên tạo thông báo mới")
        void notifyJobPostingPublished_shouldUseNewEventId_whenPostingDateChanges() {
            // Act
            service.notifyJobPostingPublished(posting(NOW.minusDays(5)), hr);
            service.notifyJobPostingPublished(posting(NOW.plusDays(2)), hr);

            // Assert
            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notifications, times(2)).save(captor.capture());
            assertNotEquals(captor.getAllValues().get(0).getEventId(), captor.getAllValues().get(1).getEventId());
        }

        @Test
        @DisplayName("Cùng tin, cùng ngày đăng gửi lại (eventId đã có) -> Không tạo thông báo trùng")
        void notifyJobPostingPublished_shouldSkip_whenEventIdAlreadyExists() {
            // Arrange
            when(notifications.existsByRecipient_UserIdAndEventId(10, "JOB_PUB_50_202610051400000000000")).thenReturn(true);

            // Act
            service.notifyJobPostingPublished(posting(LocalDateTime.of(2026, 10, 5, 14, 0)), hr);

            // Assert
            verify(notifications, never()).save(any());
        }

        @Test
        @DisplayName("Tin null / tin không có yêu cầu nguồn / yêu cầu không có HM -> Không lưu, không gọi repository")
        void notifyJobPostingPublished_shouldDoNothing_whenRecipientCannotBeFound() {
            // Arrange
            JobPosting withoutRequisition = JobPosting.builder().jobPostingId(1).build();
            JobPosting withoutManager = posting(NOW);
            withoutManager.setRequisition(JobRequisition.builder().requisitionId(2).build());

            // Act
            service.notifyJobPostingPublished(null, hr);
            service.notifyJobPostingPublished(withoutRequisition, hr);
            service.notifyJobPostingPublished(withoutManager, hr);

            // Assert
            verifyNoInteractions(notifications);
        }
    }

    // =========================================================================
    // notifyOfferLetterSent
    // =========================================================================

    @Nested
    @DisplayName("notifyOfferLetterSent")
    class NotifyOfferLetterSent {

        @Test
        @DisplayName("Gửi thư mời -> Lưu thông báo cho tài khoản ứng viên, link trang offer của ứng viên")
        void notifyOfferLetterSent_shouldSaveForCandidate_whenCandidateHasAccount() {
            // Arrange
            User candidate = candidateAccount();

            // Act
            service.notifyOfferLetterSent(offer(candidate, hr, "Senior Java Engineer"));

            // Assert
            Notification saved = savedNotification();
            assertEquals(candidate, saved.getRecipient());
            assertEquals(hr, saved.getActor());
            assertEquals("OFFER_LETTER_SENT", saved.getEventType());
            assertEquals("1", saved.getReferenceId());
            assertEquals("/portal/offers/1", saved.getLinkUrl());
            assertEquals("OFFER_SENT_1", saved.getEventId());
            assertEquals("Bạn có thư mời nhận việc mới: Senior Java Engineer", saved.getTitle());
        }

        @ParameterizedTest(name = "tên vị trí = [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Thư mời không có tên vị trí -> Ghi 'Vị trí tuyển dụng'")
        void notifyOfferLetterSent_shouldUseDefaultTitle_whenPositionTitleIsBlank(String positionTitle) {
            // Act
            service.notifyOfferLetterSent(offer(candidateAccount(), hr, positionTitle));

            // Assert
            assertEquals("Bạn có thư mời nhận việc mới: Vị trí tuyển dụng", savedNotification().getTitle());
        }

        @Test
        @DisplayName("Ứng viên chưa có tài khoản / thiếu hồ sơ / offer null -> Không lưu")
        void notifyOfferLetterSent_shouldDoNothing_whenCandidateAccountIsMissing() {
            // Arrange
            OfferProposal withoutAccount = offer(null, hr, "Java");
            OfferProposal withoutApplication = OfferProposal.builder().offerId(2).build();

            // Act
            service.notifyOfferLetterSent(null);
            service.notifyOfferLetterSent(withoutApplication);
            service.notifyOfferLetterSent(withoutAccount);

            // Assert
            verifyNoInteractions(notifications);
        }

        @Test
        @DisplayName("Cùng thư mời gửi lại (eventId đã có) -> Không tạo thông báo trùng")
        void notifyOfferLetterSent_shouldSkip_whenEventIdAlreadyExists() {
            // Arrange
            when(notifications.existsByRecipient_UserIdAndEventId(22, "OFFER_SENT_1")).thenReturn(true);

            // Act
            service.notifyOfferLetterSent(offer(candidateAccount(), hr, "Java"));

            // Assert
            verify(notifications, never()).save(any());
        }
    }

    // =========================================================================
    // notifyOfferResponseFromCandidate
    // =========================================================================

    @Nested
    @DisplayName("notifyOfferResponseFromCandidate")
    class NotifyOfferResponseFromCandidate {

        @Test
        @DisplayName("Ứng viên nhận offer -> Báo OFFER_ACCEPTED cho HR gửi thư, người gây ra là ứng viên")
        void notifyOfferResponseFromCandidate_shouldSaveAccepted_whenCandidateAccepts() {
            // Arrange
            User candidate = candidateAccount();

            // Act
            service.notifyOfferResponseFromCandidate(offer(candidate, hr, "Senior Java Engineer"), true, null);

            // Assert
            Notification saved = savedNotification();
            assertEquals(hr, saved.getRecipient());
            assertEquals(candidate, saved.getActor());
            assertEquals("OFFER_ACCEPTED", saved.getEventType());
            assertEquals("OFFER_RESP_ACC_1", saved.getEventId());
            assertEquals("/offers", saved.getLinkUrl());
            assertTrue(saved.getTitle().contains("Huỳnh Hoàng Trung (Senior Java Engineer)"));
            assertTrue(saved.getContent().contains("Đã nhận việc"));
        }

        @Test
        @DisplayName("Ứng viên từ chối có lý do -> Báo OFFER_DECLINED kèm lý do đã cắt khoảng trắng")
        void notifyOfferResponseFromCandidate_shouldIncludeTrimmedReason_whenCandidateDeclinesWithReason() {
            // Act
            service.notifyOfferResponseFromCandidate(offer(candidateAccount(), hr, "Java"), false, "  Lương thấp  ");

            // Assert
            Notification saved = savedNotification();
            assertEquals("OFFER_DECLINED", saved.getEventType());
            assertEquals("OFFER_RESP_DEC_1", saved.getEventId());
            assertTrue(saved.getContent().contains("Lý do: \"Lương thấp\"."));
        }

        @ParameterizedTest(name = "lý do = [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Ứng viên từ chối không ghi lý do -> Nội dung không có đoạn 'Lý do'")
        void notifyOfferResponseFromCandidate_shouldOmitReason_whenDeclineReasonIsBlank(String reason) {
            // Act
            service.notifyOfferResponseFromCandidate(offer(candidateAccount(), hr, "Java"), false, reason);

            // Assert
            assertFalse(savedNotification().getContent().contains("Lý do"));
        }

        @Test
        @DisplayName("Ứng viên chưa có tài khoản -> Vẫn báo HR, tên ghi 'Ứng viên', không có người gây ra")
        void notifyOfferResponseFromCandidate_shouldUseDefaultName_whenCandidateAccountIsMissing() {
            // Act
            service.notifyOfferResponseFromCandidate(offer(null, hr, "Java"), true, null);

            // Assert
            Notification saved = savedNotification();
            assertNull(saved.getActor());
            assertTrue(saved.getTitle().contains("Ứng viên (Java)"));
        }

        @Test
        @DisplayName("Offer null / không rõ HR gửi thư -> Không lưu")
        void notifyOfferResponseFromCandidate_shouldDoNothing_whenProposerIsMissing() {
            // Act
            service.notifyOfferResponseFromCandidate(null, true, null);
            service.notifyOfferResponseFromCandidate(offer(candidateAccount(), null, "Java"), false, "Lý do");

            // Assert
            verifyNoInteractions(notifications);
        }

        @Test
        @DisplayName("Nhận rồi lại gửi phản hồi nhận (eventId đã có) -> Không tạo thông báo trùng")
        void notifyOfferResponseFromCandidate_shouldSkip_whenEventIdAlreadyExists() {
            // Arrange
            when(notifications.existsByRecipient_UserIdAndEventId(30, "OFFER_RESP_ACC_1")).thenReturn(true);

            // Act
            service.notifyOfferResponseFromCandidate(offer(candidateAccount(), hr, "Java"), true, null);

            // Assert
            verify(notifications, never()).save(any());
        }
    }

    // =========================================================================
    // getNotificationsForUser / getUnreadCount
    // =========================================================================

    @Nested
    @DisplayName("getNotificationsForUser")
    class GetNotificationsForUser {

        @Test
        @DisplayName("Có thông báo -> Trả DTO đúng nội dung, chỉ truy vấn theo id người xem")
        void getNotificationsForUser_shouldMapToResponse_whenUserHasNotifications() {
            // Arrange
            Notification notification = Notification.builder().notificationId(1L).recipient(manager).title("Tiêu đề")
                    .content("Nội dung").eventType("REQUISITION_REJECTED").linkUrl("/requisitions/1")
                    .isRead(false).createdAt(NOW).build();
            when(notifications.findByRecipient_UserIdOrderByCreatedAtDesc(eq(10), any()))
                    .thenReturn(new PageImpl<>(List.of(notification)));

            // Act
            Page<NotificationResponse> result = service.getNotificationsForUser(manager, 1, 10);

            // Assert
            NotificationResponse row = result.getContent().get(0);
            assertEquals(1L, row.notificationId());
            assertEquals("Tiêu đề", row.title());
            assertEquals("/requisitions/1", row.linkUrl());
            assertFalse(row.isRead());
            assertEquals(NOW, row.createdAt());
        }

        @ParameterizedTest(name = "page={0}, size={1} -> trang {2}, cỡ {3}")
        @CsvSource({
                "1, 10, 0, 10",     // trang 1 trên giao diện = trang 0 khi truy vấn
                "3, 20, 2, 20",
                "0, 10, 0, 10",     // trang 0 / âm -> trang đầu
                "-5, 10, 0, 10",
                "1, 0, 0, 1",       // size 0 / âm -> ít nhất 1
                "1, -3, 0, 1",
                "1, 50, 0, 50",     // đúng giới hạn
                "1, 51, 0, 50",     // giới hạn + 1 -> cắt về MAX_PAGE_SIZE
                "1, 100000, 0, 50"
        })
        @DisplayName("Số trang / cỡ trang lạ -> Được chỉnh về khoảng hợp lệ trước khi truy vấn")
        void getNotificationsForUser_shouldClampPageAndSize_whenValuesAreOutOfRange(int page, int size,
                int expectedPage, int expectedSize) {
            // Arrange
            when(notifications.findByRecipient_UserIdOrderByCreatedAtDesc(eq(10), any())).thenReturn(Page.empty());

            // Act
            service.getNotificationsForUser(manager, page, size);

            // Assert
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(notifications).findByRecipient_UserIdOrderByCreatedAtDesc(eq(10), pageable.capture());
            assertEquals(expectedPage, pageable.getValue().getPageNumber());
            assertEquals(expectedSize, pageable.getValue().getPageSize());
            assertTrue(expectedSize <= NotificationServiceImpl.MAX_PAGE_SIZE);
        }

        @Test
        @DisplayName("Người xem null -> Trả trang rỗng, không gọi repository")
        void getNotificationsForUser_shouldReturnEmpty_whenUserIsNull() {
            // Act
            Page<NotificationResponse> result = service.getNotificationsForUser(null, 1, 10);

            // Assert
            assertTrue(result.isEmpty());
            verifyNoInteractions(notifications);
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCount {

        @Test
        @DisplayName("Có thông báo chưa đọc -> Trả đúng số đếm của người xem")
        void getUnreadCount_shouldReturnRepositoryCount_whenUserIsGiven() {
            // Arrange
            when(notifications.countByRecipient_UserIdAndIsReadFalse(10)).thenReturn(5L);

            // Act + Assert
            assertEquals(5L, service.getUnreadCount(manager));
        }

        @Test
        @DisplayName("Người xem null -> Trả 0, không gọi repository")
        void getUnreadCount_shouldReturnZero_whenUserIsNull() {
            // Act + Assert
            assertEquals(0L, service.getUnreadCount(null));
            verifyNoInteractions(notifications);
        }
    }

    // =========================================================================
    // markAsRead / markAllAsRead / markOfferNotificationRead
    // =========================================================================

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("Thông báo của chính mình, chưa đọc -> Đánh dấu đã đọc, thời điểm đọc = giờ hiện tại")
        void markAsRead_shouldMarkReadAtNow_whenNotificationBelongsToUser() {
            // Arrange
            Notification notification = Notification.builder().notificationId(1L).recipient(manager)
                    .linkUrl("/requisitions/101").isRead(false).build();
            when(notifications.findByNotificationIdAndRecipient_UserId(1L, 10)).thenReturn(Optional.of(notification));

            // Act
            NotificationResponse response = service.markAsRead(1L, manager);

            // Assert
            assertTrue(notification.getIsRead());
            assertEquals(NOW, notification.getReadAt());
            assertTrue(response.isRead());
            assertEquals("/requisitions/101", response.linkUrl());
        }

        @Test
        @DisplayName("Thông báo đã đọc trước đó -> Giữ nguyên thời điểm đọc đầu tiên")
        void markAsRead_shouldKeepFirstReadAt_whenAlreadyRead() {
            // Arrange
            LocalDateTime firstRead = LocalDateTime.of(2026, 10, 1, 8, 0);
            Notification notification = Notification.builder().notificationId(1L).recipient(manager)
                    .isRead(true).readAt(firstRead).build();
            when(notifications.findByNotificationIdAndRecipient_UserId(1L, 10)).thenReturn(Optional.of(notification));

            // Act
            service.markAsRead(1L, manager);

            // Assert
            assertEquals(firstRead, notification.getReadAt());
        }

        @Test
        @DisplayName("Thông báo của người khác / không tồn tại -> ResourceNotFoundException (404), không lưu gì")
        void markAsRead_shouldThrowNotFound_whenNotificationIsNotOwnedByUser() {
            // Arrange: truy vấn luôn kèm id người xem, nên thông báo của người khác = không tìm thấy
            when(notifications.findByNotificationIdAndRecipient_UserId(99L, 10)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(ResourceNotFoundException.class, () -> service.markAsRead(99L, manager));
            verify(notifications, never()).save(any());
        }

        @Test
        @DisplayName("Id null hoặc người xem null -> ResourceNotFoundException, không gọi repository")
        void markAsRead_shouldThrowNotFound_whenIdOrUserIsNull() {
            // Act + Assert
            assertThrows(ResourceNotFoundException.class, () -> service.markAsRead(null, manager));
            assertThrows(ResourceNotFoundException.class, () -> service.markAsRead(1L, null));
            verifyNoInteractions(notifications);
        }
    }

    @Nested
    @DisplayName("markAllAsRead / markOfferNotificationRead")
    class MarkAllAsRead {

        @Test
        @DisplayName("Đánh dấu tất cả -> Gọi câu cập nhật theo id người xem với giờ hiện tại")
        void markAllAsRead_shouldUpdateWithNow_whenUserIsGiven() {
            // Act
            service.markAllAsRead(manager);

            // Assert
            verify(notifications).markAllAsReadByUserId(10, NOW);
        }

        @Test
        @DisplayName("Người xem null -> Không gọi repository")
        void markAllAsRead_shouldDoNothing_whenUserIsNull() {
            // Act
            service.markAllAsRead(null);

            // Assert
            verifyNoInteractions(notifications);
        }

        @Test
        @DisplayName("Ứng viên mở offer -> Đánh dấu đã đọc thông báo offer đó với giờ hiện tại")
        void markOfferNotificationRead_shouldUpdateWithNow_whenIdsAreGiven() {
            // Act
            service.markOfferNotificationRead(22, 1);

            // Assert
            verify(notifications).markOfferNotificationRead(22, "1", NOW);
        }

        @Test
        @DisplayName("Thiếu id người dùng hoặc id offer -> Không gọi repository")
        void markOfferNotificationRead_shouldDoNothing_whenAnyIdIsNull() {
            // Act
            service.markOfferNotificationRead(null, 1);
            service.markOfferNotificationRead(22, null);

            // Assert
            verifyNoInteractions(notifications);
        }
    }
}
