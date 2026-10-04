package com.group2.rms.notification;

import com.group2.rms.requisition.entity.JobRequisition;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User manager;
    private User director;
    private JobRequisition requisition;

    @BeforeEach
    void setUp() {
        manager = User.builder().userId(10).username("hm").fullName("Hiring Manager A").build();
        director = User.builder().userId(20).username("director").fullName("Director B").build();
        requisition = JobRequisition.builder()
                .requisitionId(101)
                .title("Senior Java Engineer")
                .hiringManager(manager)
                .decidedAt(LocalDateTime.of(2026, 10, 4, 9, 30, 0, 123456700))
                .build();
    }

    @Test
    @DisplayName("notifyRequisitionRejected: Lưu thông báo đúng người nhận HM, nội dung và link")
    void notifyRequisitionRejected_success() {
        when(notificationRepository.existsByEventId(any())).thenReturn(false);

        notificationService.notifyRequisitionRejected(requisition, director, "Kinh phí dự kiến vượt trần");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(manager, saved.getRecipient());
        assertEquals("REQUISITION_REJECTED", saved.getEventType());
        assertEquals("/requisitions/101", saved.getLinkUrl());
        assertEquals("101", saved.getReferenceId());
        assertFalse(saved.getIsRead());
        assertTrue(saved.getTitle().contains("Senior Java Engineer"));
        assertTrue(saved.getContent().contains("Kinh phí dự kiến vượt trần"));
        assertTrue(saved.getContent().contains("Director B"));
        assertEquals("REQ_REJECT_101_202610040930001234567", saved.getEventId());
    }

    @Test
    @DisplayName("notifyRequisitionRejected: Idempotent - Nếu cùng eventId đã tồn tại thì không tạo lại")
    void notifyRequisitionRejected_idempotent_skipsDuplicate() {
        when(notificationRepository.existsByEventId("REQ_REJECT_101_202610040930001234567")).thenReturn(true);

        notificationService.notifyRequisitionRejected(requisition, director, "Duplicate test");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void separateDecisionsWithinOneSecond_haveDifferentEventIds() {
        notificationService.notifyRequisitionRejected(requisition, director, "First rejection");
        requisition.setDecidedAt(requisition.getDecidedAt().plusNanos(100));
        notificationService.notifyRequisitionRejected(requisition, director, "Second rejection");
        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertNotEquals(captor.getAllValues().get(0).getEventId(), captor.getAllValues().get(1).getEventId());
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

        notificationService.markAsRead(1L, manager);

        assertTrue(n.getIsRead());
        verify(notificationRepository).save(n);
    }

    @Test
    @DisplayName("markAsRead: Không tìm thấy hoặc khác user -> Ném ResponseStatusException 404")
    void markAsRead_unauthorizedOrNotFound_throwsException() {
        when(notificationRepository.findByNotificationIdAndRecipient_UserId(99L, 10)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> notificationService.markAsRead(99L, manager));
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
}
