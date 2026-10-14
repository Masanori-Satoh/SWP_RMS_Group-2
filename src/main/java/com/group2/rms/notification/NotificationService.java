package com.group2.rms.notification;

import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.User;
import org.springframework.data.domain.Page;

public interface NotificationService {

    void notifyRequisitionRejected(JobRequisition requisition, User director, String comment);

    void notifyJobPostingPublished(com.group2.rms.requisition.entity.JobPosting posting, User hr);

    void notifyOfferLetterSent(com.group2.rms.offer.entity.OfferProposal offer);

    void notifyOfferResponseFromCandidate(com.group2.rms.offer.entity.OfferProposal offer, boolean accepted, String declineReason);

    Page<NotificationResponse> getNotificationsForUser(User user, int page, int size);

    long getUnreadCount(User user);

    void markAsRead(Long notificationId, User user);

    void markOfferNotificationRead(Integer userId, Integer offerId);

    void markAllAsRead(User user);
}
