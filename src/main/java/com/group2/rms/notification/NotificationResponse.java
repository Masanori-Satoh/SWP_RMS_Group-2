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
        return from(n, true);
    }

    public static NotificationResponse from(Notification n, boolean isHrOrAdmin) {
        String resolvedLink = n.getLinkUrl();
        if (!isHrOrAdmin && resolvedLink != null && resolvedLink.startsWith("/internal/job-postings/")) {
            resolvedLink = resolvedLink.replace("/internal/job-postings/", "/jobs/");
        }
        return new NotificationResponse(
            n.getNotificationId(),
            n.getTitle(),
            n.getContent(),
            n.getEventType(),
            n.getReferenceId(),
            resolvedLink,
            Boolean.TRUE.equals(n.getIsRead()),
            n.getCreatedAt()
        );
    }
}
