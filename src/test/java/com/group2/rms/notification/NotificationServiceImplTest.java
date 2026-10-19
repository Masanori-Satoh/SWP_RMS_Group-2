package com.group2.rms.notification;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private com.group2.rms.user.repository.UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User manager;
    private User director;
    private JobRequisition requisition;
    private RequisitionApproval rejection;

    @BeforeEach
    void setUp() {
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
        com.group2.rms.candidate.entity.Candidate candidate = com.group2.rms.candidate.entity.Candidate.builder()
                .candidateId(5)
                .account(candidateUser)
                .build();
        com.group2.rms.candidate.entity.Application app = com.group2.rms.candidate.entity.Application.builder()
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
        com.group2.rms.candidate.entity.Candidate candidate = com.group2.rms.candidate.entity.Candidate.builder()
                .candidateId(5)
                .account(candidateUser)
                .build();
        com.group2.rms.candidate.entity.Application app = com.group2.rms.candidate.entity.Application.builder()
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
        assertTrue(saved.getEventId().startsWith("OFFER_RESP_ACC_1_"));
    }

    @Test
    @DisplayName("notifyOfferResponseFromCandidate: Ứng viên từ chối offer -> gửi thông báo OFFER_DECLINED kèm lý do tới HR")
    void notifyOfferResponseFromCandidate_declinedWithReason() {
        User hrUser = User.builder().userId(15).username("hr.user").fullName("Lê Thị HR").build();
        User candidateUser = User.builder().userId(22).username("trung").fullName("Huỳnh Hoàng Trung").build();
        com.group2.rms.candidate.entity.Candidate candidate = com.group2.rms.candidate.entity.Candidate.builder()
                .candidateId(5)
                .account(candidateUser)
                .build();
        com.group2.rms.candidate.entity.Application app = com.group2.rms.candidate.entity.Application.builder()
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
        assertTrue(saved.getEventId().startsWith("OFFER_RESP_DEC_1_"));
    }

    @Test
    @DisplayName("notifyOfferPendingDirector: Gửi thông báo yêu cầu duyệt offer tới tất cả active Directors")
    void notifyOfferPendingDirector_success() {
        User hrUser = User.builder().userId(15).username("hr.user").fullName("Lê Thị HR").build();
        User candidateUser = User.builder().userId(22).username("trung").fullName("Huỳnh Hoàng Trung").build();
        com.group2.rms.candidate.entity.Candidate candidate = com.group2.rms.candidate.entity.Candidate.builder()
                .candidateId(5)
                .account(candidateUser)
                .build();
        com.group2.rms.candidate.entity.Application app = com.group2.rms.candidate.entity.Application.builder()
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
        assertTrue(saved.getTitle().contains("Yêu cầu phê duyệt Đề xuất Offer"));
        assertTrue(saved.getContent().contains("Huỳnh Hoàng Trung"));
        assertTrue(saved.getEventId().startsWith("OFFER_PENDING_DIR_1_20_"));
    }

    @Test
    @DisplayName("notifyDirectorDecisionOnOffer: Giám đốc duyệt offer -> Gửi thông báo OFFER_DIRECTOR_APPROVED tới HR")
    void notifyDirectorDecisionOnOffer_approved() {
        User hrUser = User.builder().userId(15).username("hr.user").fullName("Lê Thị HR").build();
        User candidateUser = User.builder().userId(22).username("trung").fullName("Huỳnh Hoàng Trung").build();
        com.group2.rms.candidate.entity.Candidate candidate = com.group2.rms.candidate.entity.Candidate.builder()
                .candidateId(5)
                .account(candidateUser)
                .build();
        com.group2.rms.candidate.entity.Application app = com.group2.rms.candidate.entity.Application.builder()
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
        assertTrue(saved.getTitle().contains("Đề xuất Offer đã được Giám đốc phê duyệt"));
        assertTrue(saved.getContent().contains("Đồng ý mức đãi ngộ"));
        assertTrue(saved.getEventId().startsWith("OFFER_DEC_APPV_1_"));
    }

    @Test
    @DisplayName("notifyDirectorDecisionOnOffer: Giám đốc từ chối offer -> Gửi thông báo OFFER_DIRECTOR_REJECTED kèm lý do tới HR")
    void notifyDirectorDecisionOnOffer_rejected() {
        User hrUser = User.builder().userId(15).username("hr.user").fullName("Lê Thị HR").build();
        User candidateUser = User.builder().userId(22).username("trung").fullName("Huỳnh Hoàng Trung").build();
        com.group2.rms.candidate.entity.Candidate candidate = com.group2.rms.candidate.entity.Candidate.builder()
                .candidateId(5)
                .account(candidateUser)
                .build();
        com.group2.rms.candidate.entity.Application app = com.group2.rms.candidate.entity.Application.builder()
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
        assertTrue(saved.getTitle().contains("Đề xuất Offer bị Giám đốc từ chối"));
        assertTrue(saved.getContent().contains("Lương vượt ngân sách"));
        assertTrue(saved.getEventId().startsWith("OFFER_DEC_REJ_1_"));
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
}
