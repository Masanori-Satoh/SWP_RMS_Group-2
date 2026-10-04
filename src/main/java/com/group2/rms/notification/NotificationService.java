package com.group2.rms.notification;

import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.User;
import org.springframework.data.domain.Page;

public interface NotificationService {

    void notifyRequisitionRejected(JobRequisition requisition, User director, String comment);

    Page<NotificationResponse> getNotificationsForUser(User user, int page, int size);

    long getUnreadCount(User user);

    void markAsRead(Long notificationId, User user);

    void markAllAsRead(User user);
}
