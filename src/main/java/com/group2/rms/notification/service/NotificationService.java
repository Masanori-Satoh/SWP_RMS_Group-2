package com.group2.rms.notification.service;

import com.group2.rms.notification.dto.NotificationResponse;
import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.user.entity.User;
import org.springframework.data.domain.Page;

/*
 * SERVICE: tạo thông báo in-app theo từng sự kiện nghiệp vụ và đọc / đánh dấu đã đọc cho người nhận.
 */
public interface NotificationService {

    /** Báo cho Giám đốc khi HM gửi yêu cầu tuyển dụng để phê duyệt (Pending_Director). */
    void notifyRequisitionPendingDirector(JobRequisition requisition, User actor);

    /** Báo cho chủ requisition khi Giám đốc phê duyệt; {@code approval} là bản ghi phê duyệt vừa lưu. */
    void notifyRequisitionApproved(RequisitionApproval approval);

    /** Báo cho chủ requisition khi Giám đốc từ chối; {@code rejection} là bản ghi phê duyệt vừa lưu. */
    void notifyRequisitionRejected(RequisitionApproval rejection);

    /** Báo cho Giám đốc khi HM rút yêu cầu đang chờ duyệt về bản nháp. */
    void notifyRequisitionWithdrawn(JobRequisition requisition, User actor);

    void notifyJobPostingPublished(JobPosting posting, User hr);

    /** Báo cho HM chủ yêu cầu nguồn khi tin bị tạm dừng. */
    void notifyJobPostingPaused(JobPosting posting, User actor);

    /** Báo cho HM chủ yêu cầu nguồn khi tin bị đóng đợt tuyển. */
    void notifyJobPostingClosed(JobPosting posting, User actor);

    /** HR chấm Đạt: báo HM chủ yêu cầu và trưởng phòng vào duyệt hồ sơ. */
    void notifyApplicationAwaitingHiringManager(Application application, User hr);

    /** HM (hoặc Giám đốc duyệt thay) chấm Đạt: báo HR để xếp lịch phỏng vấn. */
    void notifyApplicationReadyForInterview(Application application, User reviewer);

    /** Lịch mới: báo hội đồng phỏng vấn và ứng viên. */
    void notifyInterviewScheduled(InterviewSchedule schedule, User actor);

    /** Lịch đổi giờ / địa điểm / hội đồng: báo hội đồng hiện tại và ứng viên. */
    void notifyInterviewRescheduled(InterviewSchedule schedule, User actor);

    /** Lịch bị hủy: báo hội đồng và ứng viên kèm lý do. */
    void notifyInterviewCancelled(InterviewSchedule schedule, User actor, String reason);

    /** HM chốt kết quả phỏng vấn: báo HR. */
    void notifyInterviewResultDecided(InterviewSchedule schedule, User hiringManager, String finalDecision);

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

    /** Yêu cầu bị rút lại: thông báo "chờ duyệt" của Giám đốc không còn việc để làm. */
    void markRequisitionPendingRead(Integer requisitionId);

    void markAllAsRead(User user);
}
