package com.group2.rms.notification;

import com.group2.rms.notification.constants.NotificationEventType;
import com.group2.rms.notification.constants.NotificationMessages;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service("systemNotificationService")
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository users;

    @Override
    @Transactional
    public void publish(User recipient, NotificationEventType event, String referenceType,
                        Object referenceId, String linkUrl, String eventId, boolean hrRecipient) {
        if (recipient == null || recipient.getUserId() == null || referenceId == null || eventId == null
                || !"Active".equalsIgnoreCase(recipient.getAccountStatus())) return;
        if (notificationRepository.existsByRecipient_UserIdAndEventId(recipient.getUserId(), eventId)) return;
        NotificationMessages.Message message = NotificationMessages.forEvent(event, hrRecipient);
        notificationRepository.save(Notification.builder()
                .recipient(recipient).title(message.title()).content(message.content())
                .eventType(event.name()).referenceType(referenceType)
                .referenceId(referenceId.toString()).linkUrl(linkUrl)
                .eventId(eventId).isRead(false).build());
    }

    @Override
    @Transactional
    public void publishToResponsibleHr(JobPosting posting, NotificationEventType event, String referenceType,
                                       Object referenceId, String linkUrl, String eventId) {
        if (posting == null) return;
        User creator = posting.getCreatedBy();
        if (creator != null && creator.getRole() != null
                && "HR".equalsIgnoreCase(creator.getRole().getRoleName())
                && "Active".equalsIgnoreCase(creator.getAccountStatus())) {
            publish(creator, event, referenceType, referenceId, linkUrl, eventId, true);
        } else {
            for (User hr : activeUsersWithRole("HR")) {
                publish(hr, event, referenceType, referenceId, linkUrl, eventId, true);
            }
        }
    }

    @Override
    @Transactional
    public void notifyRequisitionSubmitted(JobRequisition requisition) {
        for (User director : activeUsersWithRole("Director")) {
            publish(director, NotificationEventType.REQUISITION_SUBMITTED, "JobRequisition",
                    requisition.getRequisitionId(), "/requisitions/" + requisition.getRequisitionId(),
                    "REQ_SUBMIT_" + requisition.getRequisitionId() + "_" + requisition.getSubmittedAt(), false);
        }
    }

    @Override
    @Transactional
    public void notifyRequisitionApproved(JobRequisition requisition) {
        String eventId = "REQ_APPROVE_" + requisition.getRequisitionId() + "_" + requisition.getDecidedAt();
        String link = "/requisitions/" + requisition.getRequisitionId();
        publish(requisition.getHiringManager(), NotificationEventType.REQUISITION_APPROVED,
                "JobRequisition", requisition.getRequisitionId(), link, eventId, false);
        for (User hr : activeUsersWithRole("HR")) {
            publish(hr, NotificationEventType.REQUISITION_APPROVED,
                    "JobRequisition", requisition.getRequisitionId(), link, eventId, true);
        }
    }

    @Override
    @Transactional
    public void notifyRequisitionRejected(JobRequisition requisition, User director, String comment) {
        if (requisition == null) return;
        publish(requisition.getHiringManager(), NotificationEventType.REQUISITION_REJECTED,
                "JobRequisition", requisition.getRequisitionId(),
                "/requisitions/" + requisition.getRequisitionId(),
                "REQ_REJECT_" + requisition.getRequisitionId() + "_" + requisition.getDecidedAt(), false);
    }

    @Override
    @Transactional
    public void notifyJobPostingPublished(JobPosting posting, User hr) {
        if (posting == null || posting.getRequisition() == null) return;
        publish(posting.getRequisition().getHiringManager(), NotificationEventType.JOB_POSTING_PUBLISHED,
                "JobPosting", posting.getJobPostingId(), "/internal/job-postings/" + posting.getJobPostingId(),
                "JOB_PUBLISH_" + posting.getJobPostingId() + "_" + posting.getPostingDate(), false);
    }

    @Override
    @Transactional
    public void notifyOfferLetterSent(OfferProposal offer) {
        if (offer == null || offer.getApplication() == null || offer.getApplication().getCandidate() == null) return;
        publish(offer.getApplication().getCandidate().getAccount(), NotificationEventType.OFFER_SENT,
                "OfferProposal", offer.getOfferId(), "/portal/offers/" + offer.getOfferId(),
                "OFFER_SENT_" + offer.getOfferId(), false);
    }

    @Override
    @Transactional
    public void notifyOfferResponseFromCandidate(OfferProposal offer, boolean accepted, String declineReason) {
        if (offer == null || offer.getApplication() == null) return;
        NotificationEventType event = accepted ? NotificationEventType.OFFER_ACCEPTED : NotificationEventType.OFFER_DECLINED;
        String eventId = event.name() + "_" + offer.getOfferId();
        JobPosting posting = offer.getApplication().getJobPosting();
        User proposer = offer.getProposedBy();
        if (proposer != null && proposer.getRole() != null
                && "HR".equalsIgnoreCase(proposer.getRole().getRoleName())
                && "Active".equalsIgnoreCase(proposer.getAccountStatus())) {
            publish(proposer, event, "OfferProposal", offer.getOfferId(),
                    "/offers/" + offer.getOfferId(), eventId, true);
        } else {
            publishToResponsibleHr(posting, event, "OfferProposal", offer.getOfferId(),
                    "/offers/" + offer.getOfferId(), eventId);
        }
        if (posting != null && posting.getRequisition() != null) {
            publish(posting.getRequisition().getHiringManager(), event, "OfferProposal",
                    offer.getOfferId(), "/applications/" + offer.getApplication().getApplicationId(), eventId, false);
        }
    }

    private List<User> activeUsersWithRole(String role) {
        return users.findAllWithRoleAndDepartment().stream()
                .filter(user -> user.getRole() != null && role.equalsIgnoreCase(user.getRole().getRoleName()))
                .filter(user -> "Active".equalsIgnoreCase(user.getAccountStatus())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsForUser(User user, int page, int size) {
        if (user == null) return Page.empty();
        int validPage = Math.max(0, page - 1);
        int validSize = Math.max(1, Math.min(size, 50));
        return notificationRepository.findByRecipient_UserIdOrderByCreatedAtDesc(user.getUserId(), PageRequest.of(validPage, validSize))
                .map(NotificationResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return user == null ? 0 : notificationRepository.countByRecipient_UserIdAndIsReadFalse(user.getUserId());
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, User user) {
        if (notificationId == null || user == null) return;
        Notification notification = notificationRepository.findByNotificationIdAndRecipient_UserId(notificationId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thông báo không tồn tại hoặc không thuộc về bạn."));
        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(User user) {
        if (user != null) notificationRepository.markAllAsReadByUserId(user.getUserId());
    }

    @Override
    @Transactional
    public void markOfferNotificationRead(Integer userId, Integer offerId) {
        if (userId != null && offerId != null)
            notificationRepository.markOfferNotificationRead(userId, String.valueOf(offerId));
    }
}
