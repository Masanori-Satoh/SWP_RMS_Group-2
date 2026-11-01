package com.group2.rms.notification.repository;

import com.group2.rms.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/*
 * REPOSITORY: đọc thông báo theo người nhận, kiểm tra trùng EventId, đánh dấu đã đọc hàng loạt.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipient_UserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    long countByRecipient_UserIdAndIsReadFalse(Integer userId);

    Optional<Notification> findByNotificationIdAndRecipient_UserId(Long notificationId, Integer userId);

    boolean existsByRecipient_UserIdAndEventId(Integer recipientId, String eventId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.recipient.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Integer userId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.recipient.userId = :userId AND n.referenceId = :referenceId AND n.eventType = 'OFFER_LETTER_SENT' AND n.isRead = false")
    int markOfferNotificationRead(@Param("userId") Integer userId, @Param("referenceId") String referenceId, @Param("readAt") LocalDateTime readAt);

    /** Đánh dấu đã đọc mọi thông báo chưa đọc của một sự kiện (mọi người nhận). */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.eventType = :eventType AND n.referenceId = :referenceId AND n.isRead = false")
    int markReadByEvent(@Param("eventType") String eventType, @Param("referenceId") String referenceId, @Param("readAt") LocalDateTime readAt);
}
