package com.group2.rms.notification.service;

import com.group2.rms.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/*
 * TEST: đẩy số thông báo chưa đọc qua WebSocket cho đúng người nhận sau khi commit.
 */
@ExtendWith(MockitoExtension.class)
class NotificationPushListenerTests {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private NotificationPushListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationPushListener(notificationRepository, messagingTemplate);
    }

    @Test
    @DisplayName("push: có thông báo mới -> Gửi số chưa đọc vào kênh riêng của đúng username")
    void push_shouldSendUnreadCountToRecipient() {
        // Arrange
        when(notificationRepository.countByRecipient_UserIdAndIsReadFalse(10)).thenReturn(3L);

        // Act
        listener.push(new NotificationPushListener.Created(10, "hm"));

        // Assert
        verify(messagingTemplate).convertAndSendToUser("hm", "/queue/notifications", Map.of("unreadCount", 3L));
    }

    @Test
    @DisplayName("push: thiếu username -> Không gửi gì")
    void push_shouldSkip_whenUsernameMissing() {
        // Act
        listener.push(new NotificationPushListener.Created(10, null));

        // Assert
        verifyNoInteractions(notificationRepository, messagingTemplate);
    }

    @Test
    @DisplayName("push: gửi WebSocket lỗi -> Chỉ ghi log, không ném lỗi ra nghiệp vụ")
    void push_shouldSwallowError_whenBrokerFails() {
        // Arrange
        doThrow(new MessageDeliveryException("down"))
                .when(messagingTemplate).convertAndSendToUser(anyString(), eq("/queue/notifications"), any());

        // Act + Assert
        assertDoesNotThrow(() -> listener.push(new NotificationPushListener.Created(10, "hm")));
    }
}
