package com.group2.rms.notification.dto;

import com.group2.rms.notification.entity.Notification;
import com.group2.rms.notification.service.NotificationLabels;

import java.time.LocalDateTime;

/*
 * DTO: một dòng thông báo cho trang danh sách và chuông; nhãn / tông badge lấy từ NotificationLabels.
 */
public record NotificationResponse(
    Long notificationId,
    String title,
    String content,
    String eventType,
    String referenceId,
    String linkUrl,
    boolean isRead,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
            notification.getNotificationId(),
            notification.getTitle(),
            notification.getContent(),
            notification.getEventType(),
            notification.getReferenceId(),
            notification.getLinkUrl(),
            Boolean.TRUE.equals(notification.getIsRead()),
            notification.getCreatedAt(),
            notification.getReadAt()
        );
    }

    public String eventLabel() {
        return NotificationLabels.eventLabel(eventType);
    }

    public String eventTone() {
        return NotificationLabels.eventTone(eventType);
    }
}
