package com.group2.rms.notification.service;

import com.group2.rms.notification.dto.NotificationResponse;
import com.group2.rms.notification.entity.Notification;
import com.group2.rms.notification.repository.NotificationRepository;
import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.Nested;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.interview.entity.InterviewFormat;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.interview.entity.RoleInPanel;
import com.group2.rms.user.entity.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTests {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private com.group2.rms.user.repository.UserRepository userRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher events;

    // 30/10/2026 08:00 giờ Việt Nam
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-30T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    private NotificationServiceImpl notificationService;

    private User manager;
    private User director;
    private JobRequisition requisition;
    private RequisitionApproval rejection;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationRepository, userRepository, events, CLOCK);
        manager = User.builder().userId(10).username("hm").fullName("Hiring Manager A").build();
        director = User.builder().userId(20).username("director").fullName("Director B").build();
        requisition = JobRequisition.builder()
                .requisitionId(101)
                .title("Senior Java Engineer")
                .hiringManager(manager)
                .build();
        rejection = rejection(7, "Kinh phí dự kiến vượt trần");
    }

    private RequisitionApproval rejection(int approvalId, String comment) {
        return RequisitionApproval.builder()
                .approvalId(approvalId)
                .requisition(requisition)
                .director(director)
                .status("Rejected")
                .comments(comment)
                .approvalDate(LocalDateTime.of(2026, 10, 4, 9, 30))
                .build();
    }

    @Test
    @DisplayName("notifyRequisitionRejected: Lưu thông báo đúng người nhận HM, nội dung và link")
    void notifyRequisitionRejected_success() {
        when(notificationRepository.existsByRecipient_UserIdAndEventId(eq(10), any())).thenReturn(false);

        notificationService.notifyRequisitionRejected(rejection);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(manager, saved.getRecipient());
        assertEquals(director, saved.getActor());
        assertEquals("REQUISITION_REJECTED", saved.getEventType());
        assertEquals("/requisitions/101", saved.getLinkUrl());
        assertEquals("101", saved.getReferenceId());
        assertFalse(saved.getIsRead());
        assertTrue(saved.getTitle().contains("Senior Java Engineer"));
        assertTrue(saved.getContent().contains("Kinh phí dự kiến vượt trần"));
        assertTrue(saved.getContent().contains("Director B"));
        assertEquals("REQ_REJECT_7", saved.getEventId());
    }

    @Test
    @DisplayName("notifyRequisitionRejected: Idempotent - Nếu cùng eventId đã tồn tại thì không tạo lại")
    void notifyRequisitionRejected_idempotent_skipsDuplicate() {
        when(notificationRepository.existsByRecipient_UserIdAndEventId(10, "REQ_REJECT_7")).thenReturn(true);

        notificationService.notifyRequisitionRejected(rejection);

        verify(notificationRepository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    @DisplayName("saveIfNotDuplicate: lưu thông báo mới -> Phát sự kiện để đẩy WebSocket cho đúng người nhận")
    void saveIfNotDuplicate_shouldPublishPushEvent_whenSaved() {
        // Act
        notificationService.notifyRequisitionRejected(rejection);

        // Assert
        verify(events).publishEvent(new NotificationPushListener.Created(10, "hm"));
    }

    @Test
    @DisplayName("notifyRequisitionRejected: Từ chối lại sau khi nộp lại tạo thông báo mới")
    void separateRejections_haveDifferentEventIds() {
        notificationService.notifyRequisitionRejected(rejection(7, "First rejection"));
        notificationService.notifyRequisitionRejected(rejection(8, "Second rejection"));
        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertNotEquals(captor.getAllValues().get(0).getEventId(), captor.getAllValues().get(1).getEventId());
    }

    @Test
    @DisplayName("notifyJobPostingPublished: Lưu thông báo khi phát hành tin tuyển dụng tới HM")
    void notifyJobPostingPublished_success() {
        com.group2.rms.jobposting.entity.JobPosting posting = com.group2.rms.jobposting.entity.JobPosting.builder()
                .jobPostingId(50)
                .postingTitle("Senior Java Engineer")
                .requisition(requisition)
                .postingDate(LocalDateTime.of(2026, 10, 5, 14, 0, 0, 123456700))
                .applicationDeadline(LocalDateTime.of(2026, 11, 30, 23, 59, 59))
                .build();
        User hr = User.builder().userId(30).fullName("HR Nguyen Van C").build();

        when(notificationRepository.existsByRecipient_UserIdAndEventId(eq(10), any())).thenReturn(false);

        notificationService.notifyJobPostingPublished(posting, hr);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(manager, saved.getRecipient());
        assertEquals("JOB_POSTING_PUBLISHED", saved.getEventType());
        assertEquals("/internal/job-postings/50", saved.getLinkUrl());
        assertEquals("50", saved.getReferenceId());
        assertFalse(saved.getIsRead());
        assertTrue(saved.getTitle().contains("Tin tuyển dụng đã phát hành: Senior Java Engineer"));
        assertTrue(saved.getContent().contains("HR Nguyen Van C"));
        assertTrue(saved.getContent().contains("30/11/2026"));
        assertEquals("JOB_PUB_50_202610051400001234567", saved.getEventId());
    }

    @Test
    @DisplayName("notifyJobPostingReopened: Tiêu đề 'Tin tuyển dụng đã mở lại', eventType và eventId riêng")
    void notifyJobPostingReopened_success() {
        com.group2.rms.jobposting.entity.JobPosting posting = com.group2.rms.jobposting.entity.JobPosting.builder()
                .jobPostingId(51)
                .postingTitle("Content Creator")
                .requisition(requisition)
                .postingDate(LocalDateTime.of(2026, 10, 5, 14, 0, 0, 123456700))
                .applicationDeadline(LocalDateTime.of(2026, 11, 30, 23, 59, 59))
                .build();
        User hr = User.builder().userId(30).fullName("HR Nguyen Van C").build();

        when(notificationRepository.existsByRecipient_UserIdAndEventId(eq(10), any())).thenReturn(false);

        notificationService.notifyJobPostingReopened(posting, hr);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(manager, saved.getRecipient());
        assertEquals("Tin tuyển dụng đã mở lại: Content Creator", saved.getTitle());
        assertEquals("JOB_POSTING_REOPENED", saved.getEventType());
        assertEquals("/internal/job-postings/51", saved.getLinkUrl());
        assertTrue(saved.getContent().contains("mở lại"));
        assertTrue(saved.getContent().contains("30/11/2026"));
        assertEquals("JOB_REOPEN_51_202610051400001234567", saved.getEventId());
    }

    @Test
    @DisplayName("notifyJobPostingPublished: Idempotent - Nếu cùng eventId đã tồn tại thì bỏ qua")
    void notifyJobPostingPublished_idempotent_skipsDuplicate() {
        com.group2.rms.jobposting.entity.JobPosting posting = com.group2.rms.jobposting.entity.JobPosting.builder()
                .jobPostingId(50)
                .postingTitle("Senior Java Engineer")
                .requisition(requisition)
                .postingDate(LocalDateTime.of(2026, 10, 5, 14, 0, 0, 123456700))
                .build();
        User hr = User.builder().userId(30).fullName("HR Nguyen Van C").build();

        when(notificationRepository.existsByRecipient_UserIdAndEventId(10, "JOB_PUB_50_202610051400001234567")).thenReturn(true);

        notificationService.notifyJobPostingPublished(posting, hr);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("notifyJobPostingPublished: Không thực hiện nếu posting hoặc hiring manager là null")
    void notifyJobPostingPublished_nullPostingOrOwner_doesNothing() {
        notificationService.notifyJobPostingPublished(null, director);
        com.group2.rms.jobposting.entity.JobPosting postingNoReq = com.group2.rms.jobposting.entity.JobPosting.builder().jobPostingId(1).build();
        notificationService.notifyJobPostingPublished(postingNoReq, director);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("markAsRead: Đánh dấu đã đọc thành công khi thuộc về user")
    void markAsRead_success() {
        Notification n = Notification.builder()
                .notificationId(1L)
                .recipient(manager)
                .isRead(false)
                .build();

        when(notificationRepository.findByNotificationIdAndRecipient_UserId(1L, 10)).thenReturn(Optional.of(n));

        NotificationResponse response = notificationService.markAsRead(1L, manager);

        assertTrue(n.getIsRead());
        assertNotNull(n.getReadAt());
        assertTrue(response.isRead());
    }

    @Test
    @DisplayName("markAsRead: Đọc lại giữ nguyên thời điểm đọc đầu tiên")
    void markAsRead_twice_keepsFirstReadAt() {
        LocalDateTime firstRead = LocalDateTime.of(2026, 10, 1, 8, 0);
        Notification n = Notification.builder()
                .notificationId(1L)
                .recipient(manager)
                .isRead(true)
                .readAt(firstRead)
                .build();
        when(notificationRepository.findByNotificationIdAndRecipient_UserId(1L, 10)).thenReturn(Optional.of(n));

        notificationService.markAsRead(1L, manager);

        assertEquals(firstRead, n.getReadAt());
    }

    @Test
    @DisplayName("markAsRead: Không tìm thấy hoặc khác user -> Ném ResourceNotFoundException 404")
    void markAsRead_unauthorizedOrNotFound_throwsException() {
        when(notificationRepository.findByNotificationIdAndRecipient_UserId(99L, 10)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(99L, manager));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("getUnreadCount: Trả về số lượng chưa đọc của user")
    void getUnreadCount_returnsCount() {
        when(notificationRepository.countByRecipient_UserIdAndIsReadFalse(10)).thenReturn(5L);

        long count = notificationService.getUnreadCount(manager);
        assertEquals(5L, count);
    }

    @Test
    @DisplayName("getNotificationsForUser: Lấy danh sách map sang DTO")
    void getNotificationsForUser_mapsToDto() {
        Notification n = Notification.builder()
                .notificationId(1L)
                .recipient(manager)
                .title("Test notif")
                .content("Test content")
                .eventType("REQUISITION_REJECTED")
                .linkUrl("/requisitions/1")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByRecipient_UserIdOrderByCreatedAtDesc(eq(10), any()))
                .thenReturn(new PageImpl<>(List.of(n)));

        Page<NotificationResponse> result = notificationService.getNotificationsForUser(manager, 1, 10);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test notif", result.getContent().get(0).title());
    }

    @Test
    @DisplayName("notifyOfferLetterSent: Lưu thông báo thư mời làm việc cho ứng viên với link chuẩn")
    void notifyOfferLetterSent_success() {
        User candidateUser = User.builder().userId(22).username("trung").fullName("Huỳnh Hoàng Trung").build();
        Candidate candidate = Candidate.builder()
                .candidateId(5)
                .account(candidateUser)
                .build();
        Application app = Application.builder()
                .applicationId(10)
                .candidate(candidate)
                .build();
        com.group2.rms.offer.entity.OfferProposal offer = com.group2.rms.offer.entity.OfferProposal.builder()
                .offerId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .application(app)
                .build();

        when(notificationRepository.existsByRecipient_UserIdAndEventId(eq(22), any())).thenReturn(false);

        notificationService.notifyOfferLetterSent(offer);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(candidateUser, saved.getRecipient());
        assertEquals("OFFER_LETTER_SENT", saved.getEventType());
        assertEquals("/portal/offers/1", saved.getLinkUrl());
        assertEquals("1", saved.getReferenceId());
        assertFalse(saved.getIsRead());
        assertTrue(saved.getTitle().contains("Senior Java Engineer"));
        assertTrue(saved.getContent().contains("Senior Java Engineer"));
        assertTrue(saved.getEventId().startsWith("OFFER_SENT_1_"));
    }

    @Test
    @DisplayName("markOfferNotificationRead: Đánh dấu đã đọc thông báo offer theo userId và offerId")
    void markOfferNotificationRead_success() {
        notificationService.markOfferNotificationRead(22, 1);
        verify(notificationRepository).markOfferNotificationRead(eq(22), eq("1"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("notifyOfferResponseFromCandidate: Ứng viên nhận offer -> gửi thông báo OFFER_ACCEPTED tới HR")
    void notifyOfferResponseFromCandidate_accepted() {
        User hrUser = User.builder().userId(15).username("hr.user").fullName("Lê Thị HR").build();
        User candidateUser = User.builder().userId(22).username("trung").fullName("Huỳnh Hoàng Trung").build();
        Candidate candidate = Candidate.builder()
                .candidateId(5)
                .account(candidateUser)
                .build();
        Application app = Application.builder()
                .applicationId(10)
                .candidate(candidate)
                .build();
        com.group2.rms.offer.entity.OfferProposal offer = com.group2.rms.offer.entity.OfferProposal.builder()
                .offerId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedBy(hrUser)
                .application(app)
                .build();

        when(notificationRepository.existsByRecipient_UserIdAndEventId(eq(15), any())).thenReturn(false);

        notificationService.notifyOfferResponseFromCandidate(offer, true, null);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(hrUser, saved.getRecipient());
        assertEquals("OFFER_ACCEPTED", saved.getEventType());
        assertEquals("/offers", saved.getLinkUrl());
        assertEquals("1", saved.getReferenceId());
        assertFalse(saved.getIsRead());
        assertTrue(saved.getTitle().contains("Huỳnh Hoàng Trung"));
        assertTrue(saved.getContent().contains("Đã nhận việc"));
        assertEquals("OFFER_RESP_ACC_1", saved.getEventId());
    }

    @Test
    @DisplayName("notifyOfferResponseFromCandidate: Ứng viên từ chối offer -> gửi thông báo OFFER_DECLINED kèm lý do tới HR")
    void notifyOfferResponseFromCandidate_declinedWithReason() {
        User hrUser = User.builder().userId(15).username("hr.user").fullName("Lê Thị HR").build();
        User candidateUser = User.builder().userId(22).username("trung").fullName("Huỳnh Hoàng Trung").build();
        Candidate candidate = Candidate.builder()
                .candidateId(5)
                .account(candidateUser)
                .build();
        Application app = Application.builder()
                .applicationId(10)
                .candidate(candidate)
                .build();
        com.group2.rms.offer.entity.OfferProposal offer = com.group2.rms.offer.entity.OfferProposal.builder()
                .offerId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedBy(hrUser)
                .application(app)
                .build();

        when(notificationRepository.existsByRecipient_UserIdAndEventId(eq(15), any())).thenReturn(false);

        notificationService.notifyOfferResponseFromCandidate(offer, false, "Mức đãi ngộ chưa đạt kỳ vọng");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(hrUser, saved.getRecipient());
        assertEquals("OFFER_DECLINED", saved.getEventType());
        assertEquals("/offers", saved.getLinkUrl());
        assertEquals("1", saved.getReferenceId());
        assertFalse(saved.getIsRead());
        assertTrue(saved.getTitle().contains("Huỳnh Hoàng Trung"));
        assertTrue(saved.getContent().contains("Mức đãi ngộ chưa đạt kỳ vọng"));
        assertEquals("OFFER_RESP_DEC_1", saved.getEventId());
    }

    @Test
    @DisplayName("notifyOfferPendingDirector: Gửi thông báo yêu cầu duyệt offer tới tất cả active Directors")
    void notifyOfferPendingDirector_success() {
        User hrUser = User.builder().userId(15).username("hr.user").fullName("Lê Thị HR").build();
        User candidateUser = User.builder().userId(22).username("trung").fullName("Huỳnh Hoàng Trung").build();
        Candidate candidate = Candidate.builder()
                .candidateId(5)
                .account(candidateUser)
                .build();
        Application app = Application.builder()
                .applicationId(10)
                .candidate(candidate)
                .build();
        com.group2.rms.offer.entity.OfferProposal offer = com.group2.rms.offer.entity.OfferProposal.builder()
                .offerId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(new java.math.BigDecimal("25000000"))
                .proposedBy(hrUser)
                .application(app)
                .build();

        User directorUser = User.builder().userId(20).username("dir").fullName("Nguyen Van Director").build();
        when(userRepository.findAllActiveDirectors()).thenReturn(List.of(directorUser));
        when(notificationRepository.existsByRecipient_UserIdAndEventId(eq(20), any())).thenReturn(false);

        notificationService.notifyOfferPendingDirector(offer, hrUser);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(directorUser, saved.getRecipient());
        assertEquals(hrUser, saved.getActor());
        assertEquals("OFFER_PENDING_DIRECTOR", saved.getEventType());
        assertEquals("/offers/1", saved.getLinkUrl());
        assertEquals("1", saved.getReferenceId());
        assertTrue(saved.getTitle().contains("Yêu cầu phê duyệt Đề xuất nhận việc"));
        assertTrue(saved.getContent().contains("Huỳnh Hoàng Trung"));
        assertTrue(saved.getEventId().startsWith("OFFER_PENDING_DIR_1_20_"));
    }

    @Test
    @DisplayName("notifyDirectorDecisionOnOffer: Giám đốc duyệt offer -> Gửi thông báo OFFER_DIRECTOR_APPROVED tới HR")
    void notifyDirectorDecisionOnOffer_approved() {
        User hrUser = User.builder().userId(15).username("hr.user").fullName("Lê Thị HR").build();
        User candidateUser = User.builder().userId(22).username("trung").fullName("Huỳnh Hoàng Trung").build();
        Candidate candidate = Candidate.builder()
                .candidateId(5)
                .account(candidateUser)
                .build();
        Application app = Application.builder()
                .applicationId(10)
                .candidate(candidate)
                .build();
        com.group2.rms.offer.entity.OfferProposal offer = com.group2.rms.offer.entity.OfferProposal.builder()
                .offerId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedBy(hrUser)
                .application(app)
                .build();

        User directorUser = User.builder().userId(20).username("dir").fullName("Nguyen Van Director").build();
        when(notificationRepository.existsByRecipient_UserIdAndEventId(eq(15), any())).thenReturn(false);

        notificationService.notifyDirectorDecisionOnOffer(offer, directorUser, true, "Đồng ý mức đãi ngộ");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(hrUser, saved.getRecipient());
        assertEquals(directorUser, saved.getActor());
        assertEquals("OFFER_DIRECTOR_APPROVED", saved.getEventType());
        assertEquals("/offers/1", saved.getLinkUrl());
        assertTrue(saved.getTitle().contains("Đề xuất nhận việc đã được Giám đốc phê duyệt"));
        assertTrue(saved.getContent().contains("Đồng ý mức đãi ngộ"));
        assertTrue(saved.getEventId().startsWith("OFFER_DEC_APPV_1_"));
    }

    @Test
    @DisplayName("notifyDirectorDecisionOnOffer: Giám đốc từ chối offer -> Gửi thông báo OFFER_DIRECTOR_REJECTED kèm lý do tới HR")
    void notifyDirectorDecisionOnOffer_rejected() {
        User hrUser = User.builder().userId(15).username("hr.user").fullName("Lê Thị HR").build();
        User candidateUser = User.builder().userId(22).username("trung").fullName("Huỳnh Hoàng Trung").build();
        Candidate candidate = Candidate.builder()
                .candidateId(5)
                .account(candidateUser)
                .build();
        Application app = Application.builder()
                .applicationId(10)
                .candidate(candidate)
                .build();
        com.group2.rms.offer.entity.OfferProposal offer = com.group2.rms.offer.entity.OfferProposal.builder()
                .offerId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedBy(hrUser)
                .application(app)
                .build();

        User directorUser = User.builder().userId(20).username("dir").fullName("Nguyen Van Director").build();
        when(notificationRepository.existsByRecipient_UserIdAndEventId(eq(15), any())).thenReturn(false);

        notificationService.notifyDirectorDecisionOnOffer(offer, directorUser, false, "Lương vượt ngân sách");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(hrUser, saved.getRecipient());
        assertEquals(directorUser, saved.getActor());
        assertEquals("OFFER_DIRECTOR_REJECTED", saved.getEventType());
        assertEquals("/offers/1", saved.getLinkUrl());
        assertTrue(saved.getTitle().contains("Đề xuất nhận việc bị Giám đốc từ chối"));
        assertTrue(saved.getContent().contains("Lương vượt ngân sách"));
        assertTrue(saved.getEventId().startsWith("OFFER_DEC_REJ_1_"));
    }

    @Test
    @DisplayName("notifyDirectorDecisionOnOffer: Phát thông báo phê duyệt tới cả người tạo và tất cả chuyên viên HR active")
    void notifyDirectorDecisionOnOffer_broadcastToAllActiveHrs() {
        User hrCreator = User.builder().userId(15).username("hr.creator").fullName("HR Creator").build();
        User hrColleague = User.builder().userId(16).username("hr.colleague").fullName("HR Colleague").build();
        User candidateUser = User.builder().userId(22).username("candidate").fullName("Candidate Name").build();
        Candidate candidate = Candidate.builder().candidateId(5).account(candidateUser).build();
        Application app = Application.builder().applicationId(10).candidate(candidate).build();
        com.group2.rms.offer.entity.OfferProposal offer = com.group2.rms.offer.entity.OfferProposal.builder()
                .offerId(99)
                .offeredPositionTitle("Frontend ReactJS Developer")
                .proposedBy(hrCreator)
                .application(app)
                .build();

        User directorUser = User.builder().userId(20).username("dir").fullName("Nguyen Van Director").build();
        when(userRepository.findAllActiveHrs()).thenReturn(List.of(hrCreator, hrColleague));
        when(notificationRepository.existsByRecipient_UserIdAndEventId(any(), any())).thenReturn(false);

        notificationService.notifyDirectorDecisionOnOffer(offer, directorUser, true, "Đã duyệt");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());

        List<Notification> savedList = captor.getAllValues();
        assertEquals(2, savedList.size());
        assertTrue(savedList.stream().anyMatch(n -> n.getRecipient().getUserId().equals(15)));
        assertTrue(savedList.stream().anyMatch(n -> n.getRecipient().getUserId().equals(16)));
    }

    // =========================================================================
    // notifyRequisitionPendingDirector
    // =========================================================================

    @Test
    @DisplayName("notifyRequisitionPendingDirector: Gửi thông báo REQUISITION_PENDING_DIRECTOR tới các Giám đốc đang hoạt động")
    void notifyRequisitionPendingDirector_success() {
        requisition.setRequisitionCode("REQ-0101");
        requisition.setSubmittedAt(LocalDateTime.of(2026, 10, 19, 10, 0));
        com.group2.rms.user.entity.Department dept = com.group2.rms.user.entity.Department.builder()
                .departmentId(1).departmentName("Phòng Công Nghệ").build();
        requisition.setDepartment(dept);

        User director1 = User.builder().userId(20).fullName("Giám đốc A").build();
        User director2 = User.builder().userId(21).fullName("Giám đốc B").build();
        when(userRepository.findAllActiveDirectors()).thenReturn(List.of(director1, director2));
        when(notificationRepository.existsByRecipient_UserIdAndEventId(any(), any())).thenReturn(false);

        notificationService.notifyRequisitionPendingDirector(requisition, manager);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());

        List<Notification> savedList = captor.getAllValues();
        assertEquals(director1, savedList.get(0).getRecipient());
        assertEquals(director2, savedList.get(1).getRecipient());
        assertEquals(manager, savedList.get(0).getActor());
        assertEquals("REQUISITION_PENDING_DIRECTOR", savedList.get(0).getEventType());
        assertEquals("/requisitions/101", savedList.get(0).getLinkUrl());
        assertEquals("101", savedList.get(0).getReferenceId());
        assertTrue(savedList.get(0).getTitle().contains("Senior Java Engineer"));
        assertTrue(savedList.get(0).getContent().contains("Hiring Manager A"));
        assertTrue(savedList.get(0).getContent().contains("REQ-0101"));
        assertTrue(savedList.get(0).getContent().contains("Phòng Công Nghệ"));
        assertTrue(savedList.get(0).getEventId().startsWith("REQ_PENDING_DIR_101_20_"));
        assertTrue(savedList.get(1).getEventId().startsWith("REQ_PENDING_DIR_101_21_"));
    }

    @Test
    @DisplayName("notifyRequisitionPendingDirector: Bỏ qua Giám đốc nếu chính Giám đốc đó là người gửi")
    void notifyRequisitionPendingDirector_skipsSelfWhenDirectorIsActor() {
        requisition.setSubmittedAt(LocalDateTime.of(2026, 10, 19, 10, 0));
        User director1 = User.builder().userId(20).fullName("Giám đốc A").build();
        User director2 = User.builder().userId(21).fullName("Giám đốc B").build();
        when(userRepository.findAllActiveDirectors()).thenReturn(List.of(director1, director2));
        when(notificationRepository.existsByRecipient_UserIdAndEventId(eq(21), any())).thenReturn(false);

        // Director1 là người nộp
        notificationService.notifyRequisitionPendingDirector(requisition, director1);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());
        assertEquals(director2, captor.getValue().getRecipient());
    }

    @Test
    @DisplayName("notifyRequisitionPendingDirector: Idempotent - Nếu cùng eventId đã tồn tại thì bỏ qua")
    void notifyRequisitionPendingDirector_idempotent() {
        requisition.setSubmittedAt(LocalDateTime.of(2026, 10, 19, 10, 0));
        when(userRepository.findAllActiveDirectors()).thenReturn(List.of(director));
        when(notificationRepository.existsByRecipient_UserIdAndEventId(eq(20), any())).thenReturn(true);

        notificationService.notifyRequisitionPendingDirector(requisition, manager);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("notifyRequisitionPendingDirector: requisition null hoặc không có Giám đốc hoạt động -> Không lưu")
    void notifyRequisitionPendingDirector_nullOrNoDirectors_doesNothing() {
        notificationService.notifyRequisitionPendingDirector(null, manager);

        when(userRepository.findAllActiveDirectors()).thenReturn(List.of());
        notificationService.notifyRequisitionPendingDirector(requisition, manager);

        verify(notificationRepository, never()).save(any());
    }

    // =========================================================================
    // notifyRequisitionApproved
    // =========================================================================

    @Test
    @DisplayName("notifyRequisitionApproved: Giám đốc duyệt -> Gửi thông báo REQUISITION_APPROVED tới Hiring Manager")
    void notifyRequisitionApproved_success() {
        RequisitionApproval approval = RequisitionApproval.builder()
                .approvalId(12)
                .requisition(requisition)
                .director(director)
                .status("Approved")
                .comments("Đồng ý tuyển dụng")
                .approvalDate(LocalDateTime.of(2026, 10, 19, 14, 0))
                .build();

        when(notificationRepository.existsByRecipient_UserIdAndEventId(eq(10), any())).thenReturn(false);

        notificationService.notifyRequisitionApproved(approval);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(manager, saved.getRecipient());
        assertEquals(director, saved.getActor());
        assertEquals("REQUISITION_APPROVED", saved.getEventType());
        assertEquals("/requisitions/101", saved.getLinkUrl());
        assertEquals("101", saved.getReferenceId());
        assertTrue(saved.getTitle().contains("Yêu cầu tuyển dụng đã được phê duyệt"));
        assertTrue(saved.getContent().contains("Director B"));
        assertTrue(saved.getContent().contains("Đồng ý tuyển dụng"));
        assertEquals("REQ_APPROVE_12", saved.getEventId());
    }

    @Test
    @DisplayName("notifyRequisitionApproved: Giám đốc duyệt không kèm ý kiến -> Thông báo vẫn đầy đủ nội dung chuẩn")
    void notifyRequisitionApproved_withoutComment() {
        RequisitionApproval approval = RequisitionApproval.builder()
                .approvalId(13)
                .requisition(requisition)
                .director(director)
                .status("Approved")
                .comments(null)
                .approvalDate(LocalDateTime.of(2026, 10, 19, 14, 0))
                .build();

        when(notificationRepository.existsByRecipient_UserIdAndEventId(eq(10), any())).thenReturn(false);

        notificationService.notifyRequisitionApproved(approval);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertTrue(saved.getContent().contains("Bộ phận Nhân sự (HR) có thể tiến hành tạo tin tuyển dụng"));
        assertFalse(saved.getContent().contains("Ý kiến:"));
        assertEquals("REQ_APPROVE_13", saved.getEventId());
    }

    @Test
    @DisplayName("notifyRequisitionApproved: Giám đốc tự duyệt yêu cầu của mình -> Không gửi thông báo cho chính mình")
    void notifyRequisitionApproved_skipsSelfApproval() {
        requisition.setHiringManager(director);
        RequisitionApproval approval = RequisitionApproval.builder()
                .approvalId(14)
                .requisition(requisition)
                .director(director)
                .status("Approved")
                .build();

        notificationService.notifyRequisitionApproved(approval);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("notifyRequisitionApproved: Idempotent - Nếu cùng eventId đã tồn tại thì bỏ qua")
    void notifyRequisitionApproved_idempotent() {
        RequisitionApproval approval = RequisitionApproval.builder()
                .approvalId(15)
                .requisition(requisition)
                .director(director)
                .status("Approved")
                .build();

        when(notificationRepository.existsByRecipient_UserIdAndEventId(10, "REQ_APPROVE_15")).thenReturn(true);

        notificationService.notifyRequisitionApproved(approval);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("notifyRequisitionApproved: null approval hoặc requisition null -> Không làm gì")
    void notifyRequisitionApproved_null_doesNothing() {
        notificationService.notifyRequisitionApproved(null);
        notificationService.notifyRequisitionApproved(RequisitionApproval.builder().build());

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("notifyRequisitionApproved: HR đang hoạt động cũng nhận thông báo để tạo tin, kể cả khi Giám đốc tự duyệt")
    void notifyRequisitionApproved_shouldNotifyActiveHrs_whenRequisitionApproved() {
        // Arrange
        requisition.setHiringManager(director);
        User hr = User.builder().userId(30).username("hr").fullName("HR C").build();
        RequisitionApproval approval = RequisitionApproval.builder()
                .approvalId(16).requisition(requisition).director(director).status("Approved").build();
        when(userRepository.findAllActiveHrs()).thenReturn(List.of(hr));

        // Act
        notificationService.notifyRequisitionApproved(approval);

        // Assert
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(hr, saved.getRecipient());
        assertEquals("REQUISITION_APPROVED", saved.getEventType());
        assertEquals("/internal/job-postings/create?requisitionId=101", saved.getLinkUrl());
        assertEquals("REQ_APPROVE_16", saved.getEventId());
    }

    @Test
    @DisplayName("notifyRequisitionApproved: thiếu ngày duyệt -> Lấy giờ từ Clock, không dùng giờ máy")
    void notifyRequisitionApproved_shouldUseClock_whenApprovalDateMissing() {
        // Arrange
        RequisitionApproval approval = RequisitionApproval.builder()
                .approvalId(17).requisition(requisition).director(director).status("Approved").build();

        // Act
        notificationService.notifyRequisitionApproved(approval);

        // Assert
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertTrue(captor.getValue().getContent().contains("30/10/2026 08:00"));
    }

    @Test
    @DisplayName("notifyOfferResponseFromCandidate: gọi lại cùng phản hồi -> Cùng eventId (chống trùng được)")
    void notifyOfferResponseFromCandidate_shouldUseStableEventId_whenCalledTwice() {
        // Arrange
        User hrUser = User.builder().userId(15).username("hr.user").fullName("Lê Thị HR").build();
        com.group2.rms.offer.entity.OfferProposal offer = com.group2.rms.offer.entity.OfferProposal.builder()
                .offerId(3).offeredPositionTitle("Tester").proposedBy(hrUser).build();

        // Act
        notificationService.notifyOfferResponseFromCandidate(offer, true, null);
        notificationService.notifyOfferResponseFromCandidate(offer, true, null);

        // Assert
        verify(notificationRepository, times(2)).existsByRecipient_UserIdAndEventId(15, "OFFER_RESP_ACC_3");
    }

    @Test
    @DisplayName("markRequisitionPendingRead: rút lại yêu cầu -> Đánh dấu đã đọc thông báo chờ duyệt của yêu cầu đó")
    void markRequisitionPendingRead_shouldMarkPendingDirectorNotifications() {
        // Act
        notificationService.markRequisitionPendingRead(101);

        // Assert
        verify(notificationRepository).markReadByEvent("REQUISITION_PENDING_DIRECTOR", "101",
                LocalDateTime.of(2026, 10, 30, 8, 0));
    }

    // =========================================================================
    // N1–N7: rút yêu cầu, tạm dừng / đóng tin, duyệt hồ sơ, lịch và kết quả phỏng vấn
    // =========================================================================

    @Nested
    @DisplayName("Thông báo luồng bổ sung N1–N7")
    class WorkflowNotifications {

        private final User hr = User.builder().userId(30).fullName("Lan HR").build();
        private final User otherHr = User.builder().userId(31).fullName("Quang HR").build();
        private final User interviewer = User.builder().userId(40).fullName("Minh Interviewer").build();
        private final User candidateUser = User.builder().userId(50).fullName("Nguyễn Văn Ứng").build();

        private JobPosting posting() {
            return JobPosting.builder().jobPostingId(77).postingTitle("Java Developer").requisition(requisition).build();
        }

        private Application application() {
            return Application.builder().applicationId(88)
                    .candidate(Candidate.builder().candidateId(5).account(candidateUser).build())
                    .jobPosting(posting()).build();
        }

        private InterviewSchedule schedule() {
            InterviewSchedule schedule = InterviewSchedule.builder()
                    .interviewId(9L).application(application())
                    .interviewFormat(InterviewFormat.Offline_Office)
                    .startTime(LocalDateTime.of(2026, 11, 2, 9, 0)).endTime(LocalDateTime.of(2026, 11, 2, 10, 0))
                    .locationOrLink("Phòng 301").build();
            schedule.addPanelMember(interviewer, RoleInPanel.Interviewer);
            schedule.addPanelMember(hr, RoleInPanel.HR);
            return schedule;
        }

        private List<Notification> savedNotifications(int count) {
            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, times(count)).save(captor.capture());
            return captor.getAllValues();
        }

        @Test
        @DisplayName("notifyRequisitionWithdrawn: HM rút yêu cầu -> Mọi Giám đốc nhận thông báo, link về yêu cầu")
        void notifyRequisitionWithdrawn_shouldNotifyAllDirectors_whenManagerWithdraws() {
            // Arrange
            User otherDirector = User.builder().userId(21).fullName("Director C").build();
            when(userRepository.findAllActiveDirectors()).thenReturn(List.of(director, otherDirector));

            // Act
            notificationService.notifyRequisitionWithdrawn(requisition, manager);

            // Assert
            List<Notification> saved = savedNotifications(2);
            assertEquals(List.of(director, otherDirector), saved.stream().map(Notification::getRecipient).toList());
            assertEquals("REQUISITION_WITHDRAWN", saved.get(0).getEventType());
            assertEquals("/requisitions/101", saved.get(0).getLinkUrl());
            assertTrue(saved.get(0).getContent().contains("Hiring Manager A"));
        }

        @Test
        @DisplayName("notifyRequisitionWithdrawn: Giám đốc tự rút yêu cầu của mình -> Không báo cho chính mình")
        void notifyRequisitionWithdrawn_shouldSkipActor_whenDirectorWithdraws() {
            // Arrange
            when(userRepository.findAllActiveDirectors()).thenReturn(List.of(director));

            // Act
            notificationService.notifyRequisitionWithdrawn(requisition, director);

            // Assert
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("notifyJobPostingPaused / Closed: HR tạm dừng, đóng tin -> HM chủ yêu cầu nhận, link tin nội bộ")
        void notifyJobPostingPausedAndClosed_shouldNotifyRequisitionOwner() {
            // Act
            notificationService.notifyJobPostingPaused(posting(), hr);
            notificationService.notifyJobPostingClosed(posting(), hr);

            // Assert
            List<Notification> saved = savedNotifications(2);
            assertEquals(manager, saved.get(0).getRecipient());
            assertEquals("JOB_POSTING_PAUSED", saved.get(0).getEventType());
            assertEquals("JOB_POSTING_CLOSED", saved.get(1).getEventType());
            assertEquals("/internal/job-postings/77", saved.get(1).getLinkUrl());
            assertTrue(saved.get(1).getContent().startsWith("Lan HR đã đóng"));
        }

        @Test
        @DisplayName("notifyJobPostingPaused: chính HM tạm dừng -> Không báo cho chính mình")
        void notifyJobPostingPaused_shouldSkip_whenActorIsOwner() {
            // Act
            notificationService.notifyJobPostingPaused(posting(), manager);

            // Assert
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("notifyApplicationAwaitingHiringManager: HR chấm Đạt -> HM chủ yêu cầu và trưởng phòng (khác người) đều nhận")
        void notifyApplicationAwaitingHiringManager_shouldNotifyOwnerAndDepartmentManager() {
            // Arrange
            User departmentHead = User.builder().userId(11).fullName("Trưởng phòng D").build();
            requisition.setDepartment(Department.builder().departmentId(3).departmentName("IT").manager(departmentHead).build());

            // Act
            notificationService.notifyApplicationAwaitingHiringManager(application(), hr);

            // Assert
            List<Notification> saved = savedNotifications(2);
            assertEquals(List.of(manager, departmentHead), saved.stream().map(Notification::getRecipient).toList());
            assertEquals("APPLICATION_HR_PASSED", saved.get(0).getEventType());
            assertEquals("/applications/88", saved.get(0).getLinkUrl());
            assertEquals("APP_HR_PASS_88", saved.get(0).getEventId());
            assertTrue(saved.get(0).getContent().contains("Nguyễn Văn Ứng"));
        }

        @Test
        @DisplayName("notifyApplicationAwaitingHiringManager: HM chủ cũng là trưởng phòng -> Chỉ một thông báo")
        void notifyApplicationAwaitingHiringManager_shouldNotDuplicate_whenOwnerIsDepartmentManager() {
            // Arrange
            requisition.setDepartment(Department.builder().departmentId(3).departmentName("IT").manager(manager).build());

            // Act
            notificationService.notifyApplicationAwaitingHiringManager(application(), hr);

            // Assert
            assertEquals(manager, savedNotifications(1).get(0).getRecipient());
        }

        @Test
        @DisplayName("notifyApplicationReadyForInterview: HM chấm Đạt -> Mọi HR nhận, link về hồ sơ")
        void notifyApplicationReadyForInterview_shouldNotifyAllHrs() {
            // Arrange
            when(userRepository.findAllActiveHrs()).thenReturn(List.of(hr, otherHr));

            // Act
            notificationService.notifyApplicationReadyForInterview(application(), manager);

            // Assert
            List<Notification> saved = savedNotifications(2);
            assertEquals(List.of(hr, otherHr), saved.stream().map(Notification::getRecipient).toList());
            assertEquals("APPLICATION_HM_PASSED", saved.get(0).getEventType());
            assertEquals("APP_HM_PASS_88", saved.get(0).getEventId());
            assertEquals(manager, saved.get(0).getActor());
        }

        @Test
        @DisplayName("notifyInterviewScheduled: HR tạo lịch -> Hội đồng (trừ HR tạo) và ứng viên nhận, link đúng phía")
        void notifyInterviewScheduled_shouldNotifyPanelAndCandidate_exceptActor() {
            // Act
            notificationService.notifyInterviewScheduled(schedule(), hr);

            // Assert
            List<Notification> saved = savedNotifications(2);
            Notification panel = saved.get(0);
            Notification candidate = saved.get(1);
            assertEquals(interviewer, panel.getRecipient());
            assertEquals("/interviews", panel.getLinkUrl());
            assertTrue(panel.getContent().contains("02/11/2026 09:00 – 10:00"));
            assertTrue(panel.getContent().contains("Phòng 301"));
            assertEquals(candidateUser, candidate.getRecipient());
            assertEquals("/portal/interviews", candidate.getLinkUrl());
            assertEquals("INTERVIEW_SCHEDULED", candidate.getEventType());
            assertEquals("INT_SCHED_9", candidate.getEventId());
        }

        @Test
        @DisplayName("notifyInterviewCancelled: có lý do -> Lý do chỉ ghi cho hội đồng, ứng viên không thấy")
        void notifyInterviewCancelled_shouldShowReasonOnlyToPanel() {
            // Act
            notificationService.notifyInterviewCancelled(schedule(), hr, "Ứng viên xin dời");

            // Assert
            List<Notification> saved = savedNotifications(2);
            assertTrue(saved.get(0).getContent().contains("Ứng viên xin dời"));
            assertFalse(saved.get(1).getContent().contains("Ứng viên xin dời"));
            assertEquals("INTERVIEW_CANCELLED", saved.get(1).getEventType());
        }

        @Test
        @DisplayName("notifyInterviewRescheduled: đổi lịch -> eventId kèm thời điểm đổi (mỗi lần đổi một thông báo)")
        void notifyInterviewRescheduled_shouldUseTimeInEventId() {
            // Act
            notificationService.notifyInterviewRescheduled(schedule(), hr);

            // Assert
            assertEquals("INT_RESCHED_9_202610300800000000000", savedNotifications(2).get(0).getEventId());
        }

        @Test
        @DisplayName("notifyInterviewResultDecided: HM chốt Đạt -> Mọi HR nhận, nội dung có kết quả tiếng Việt")
        void notifyInterviewResultDecided_shouldNotifyHrsWithLabel() {
            // Arrange
            when(userRepository.findAllActiveHrs()).thenReturn(List.of(hr));

            // Act
            notificationService.notifyInterviewResultDecided(schedule(), manager, "Passed");

            // Assert
            Notification saved = savedNotifications(1).get(0);
            assertEquals(hr, saved.getRecipient());
            assertEquals("/interviews/9/result", saved.getLinkUrl());
            assertEquals("INT_RESULT_9_Passed", saved.getEventId());
            assertTrue(saved.getContent().endsWith(": Đạt."));
        }

        @Test
        @DisplayName("NotificationLabels: loại đã khai báo có nhãn và tông; loại lạ -> Ẩn badge, tông neutral")
        void notificationLabels_shouldTranslateKnownTypes() {
            assertEquals("Hủy lịch phỏng vấn", NotificationLabels.eventLabel("INTERVIEW_CANCELLED"));
            assertEquals("danger", NotificationLabels.eventTone("INTERVIEW_CANCELLED"));
            assertNull(NotificationLabels.eventLabel("UNKNOWN"));
            assertEquals("neutral", NotificationLabels.eventTone(null));
        }
    }
}
