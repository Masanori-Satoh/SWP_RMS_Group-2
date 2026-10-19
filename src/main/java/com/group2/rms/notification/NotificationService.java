package com.group2.rms.notification;

import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.user.entity.User;
import org.springframework.data.domain.Page;

public interface NotificationService {

    /** Báo cho chủ requisition khi Giám đốc từ chối; {@code rejection} là bản ghi phê duyệt vừa lưu. */
    void notifyRequisitionRejected(RequisitionApproval rejection);

    void notifyJobPostingPublished(JobPosting posting, User hr);

    void notifyOfferLetterSent(OfferProposal offer);

    /** Báo cho Giám đốc khi HR gửi đề xuất Offer để phê duyệt (Pending_Director). */
    void notifyOfferPendingDirector(OfferProposal offer, User hrActor);

    /** Báo cho HR đề xuất khi Giám đốc phê duyệt hoặc từ chối Offer. */
    void notifyDirectorDecisionOnOffer(OfferProposal offer, User director, boolean approved, String comments);

    void notifyOfferResponseFromCandidate(OfferProposal offer, boolean accepted, String declineReason);

    Page<NotificationResponse> getNotificationsForUser(User user, int page, int size);

    long getUnreadCount(User user);

    /** Đánh dấu đã đọc (idempotent) và trả về thông báo để điều hướng tới liên kết do server tạo. */
    NotificationResponse markAsRead(Long notificationId, User user);

    void markOfferNotificationRead(Integer userId, Integer offerId);

    void markAllAsRead(User user);
}
