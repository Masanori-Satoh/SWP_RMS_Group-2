package com.group2.rms.notification;

import com.group2.rms.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificationId")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RecipientId", nullable = false, referencedColumnName = "UserId")
    private User recipient;

    /**
     * Người gây ra sự kiện; NULL = hệ thống tự sinh.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ActorId", referencedColumnName = "UserId")
    private User actor;

    @Column(name = "Title", nullable = false, length = 255)
    private String title;

    @Column(name = "Content", nullable = false, length = 1000)
    private String content;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "EventType", nullable = false, length = 50)
    private String eventType;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "ReferenceId", length = 50)
    private String referenceId;

    @Column(name = "LinkUrl", length = 255)
    private String linkUrl;

    @Column(name = "IsRead", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "ReadAt")
    private LocalDateTime readAt;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "EventId", length = 100)
    private String eventId;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    /** Đánh dấu đã đọc, giữ nguyên thời điểm đọc đầu tiên. */
    public void markRead(LocalDateTime now) {
        if (!Boolean.TRUE.equals(isRead)) {
            isRead = true;
            readAt = now;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isRead == null) {
            isRead = false;
        }
    }
}
