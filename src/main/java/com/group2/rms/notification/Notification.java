package com.group2.rms.notification;

import com.group2.rms.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Notification", indexes = {
    @Index(name = "IX_Notification_Recipient_IsRead", columnList = "RecipientId, IsRead"),
    @Index(name = "IX_Notification_EventId", columnList = "EventId")
})
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

    @Column(name = "Title", nullable = false, length = 255)
    private String title;

    @Column(name = "Content", nullable = false, length = 1000)
    private String content;

    @Column(name = "EventType", nullable = false, length = 50)
    private String eventType;

    @Column(name = "ReferenceId", length = 50)
    private String referenceId;

    @Column(name = "LinkUrl", length = 255)
    private String linkUrl;

    @Column(name = "IsRead", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "EventId", length = 100, unique = true)
    private String eventId;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

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
