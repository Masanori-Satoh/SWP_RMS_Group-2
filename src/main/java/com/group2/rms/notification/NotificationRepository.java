package com.group2.rms.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipient_UserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    long countByRecipient_UserIdAndIsReadFalse(Integer userId);

    Optional<Notification> findByNotificationIdAndRecipient_UserId(Long notificationId, Integer userId);

    default Optional<Notification> findByIdAndRecipient_UserId(Long notificationId, Integer userId) {
        return findByNotificationIdAndRecipient_UserId(notificationId, userId);
    }

    boolean existsByEventId(String eventId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Integer userId);
}
