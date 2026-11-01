package com.group2.rms.notification.service;

import com.group2.rms.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/*
 * LISTENER: sau khi lưu thông báo commit xong thì đẩy số chưa đọc mới tới trình duyệt người nhận qua WebSocket.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPushListener {

    /** Kênh riêng của từng người; trình duyệt nghe "/user" + kênh này. */
    public static final String DESTINATION = "/queue/notifications";

    /** Sự kiện phát ra ngay khi lưu một thông báo (lúc đó transaction chưa commit). */
    public record Created(Integer recipientId, String recipientUsername) {
    }

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Rollback thì không đẩy; đẩy lỗi chỉ ghi log vì trình duyệt vẫn tự hỏi lại server định kỳ
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void push(Created event) {
        if (event.recipientId() == null || event.recipientUsername() == null) {
            return;
        }
        try {
            long unread = notificationRepository.countByRecipient_UserIdAndIsReadFalse(event.recipientId());
            messagingTemplate.convertAndSendToUser(event.recipientUsername(), DESTINATION, Map.of("unreadCount", unread));
        } catch (RuntimeException ex) {
            log.warn("Could not push notification to user {}: {}", event.recipientId(), ex.getMessage());
        }
    }
}
