package com.group2.rms.notification;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long notificationId,
    String title,
    String content,
    String eventType,
    String referenceId,
    String linkUrl,
    boolean isRead,
    LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
            n.getNotificationId(),
            n.getTitle(),
            n.getContent(),
            n.getEventType(),
            n.getReferenceId(),
            n.getLinkUrl(),
            Boolean.TRUE.equals(n.getIsRead()),
            n.getCreatedAt()
        );
    }
}
