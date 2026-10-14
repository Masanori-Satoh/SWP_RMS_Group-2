package com.group2.rms.notification;

import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.User;
import com.group2.rms.notification.constants.NotificationEventType;
import org.springframework.data.domain.Page;

public interface NotificationService {

    void publish(User recipient, NotificationEventType event, String referenceType,
                 Object referenceId, String linkUrl, String eventId, boolean hrRecipient);

    void publishToResponsibleHr(JobPosting posting, NotificationEventType event, String referenceType,
                                Object referenceId, String linkUrl, String eventId);

    void notifyRequisitionRejected(JobRequisition requisition, User director, String comment);

    void notifyRequisitionSubmitted(JobRequisition requisition);

    void notifyRequisitionApproved(JobRequisition requisition);

    void notifyJobPostingPublished(JobPosting posting, User hr);

    void notifyOfferLetterSent(OfferProposal offer);

    void notifyOfferResponseFromCandidate(OfferProposal offer, boolean accepted, String declineReason);

    Page<NotificationResponse> getNotificationsForUser(User user, int page, int size);

    long getUnreadCount(User user);

    void markAsRead(Long notificationId, User user);

    void markOfferNotificationRead(Integer userId, Integer offerId);

    void markAllAsRead(User user);
}
