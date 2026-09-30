package com.rms.feature.notification;

import com.group2.rms.entity.OfferProposal;

/**
 * Service quản lý gửi thông báo (email, in-app notification) cho module Offer.
 */
public interface NotificationService {

    /**
     * Báo cho Director có offer chờ duyệt.
     */
    void sendOfferSubmittedNotificationToDirector(OfferProposal offer);

    /**
     * Báo cho HR khi Director đã phê duyệt Offer, để HR thực hiện bước phát hành gửi tới ứng viên.
     */
    void sendOfferApprovedNotificationToHR(OfferProposal offer);

    /**
     * Gửi email thư mời làm việc cho Candidate SAU KHI HR đã phát hành Offer (Sent_Candidate).
     */
    void sendOfferApprovedNotificationToCandidate(OfferProposal offer);

    /**
     * Báo cho HR/Hiring Manager khi Candidate phản hồi (Accept / Decline / Negotiate).
     */
    void sendCandidateResponseNotificationToHR(OfferProposal offer, String action);

    /**
     * Báo cho Candidate và Hiring Manager khi HR phản hồi yêu cầu thương lượng (backward compatibility).
     */
    void sendHrNegotiationResponseNotification(OfferProposal offer, String hrResponseNotes);

    /**
     * Báo cho Hiring Manager khi HR DUYỆT yêu cầu thương lượng của ứng viên.
     * HM nhận thông báo kèm mức lương ứng viên đề xuất và chỉ đạo từ HR để điều chỉnh lại Offer.
     */
    void sendHrNegotiationApprovedNotificationToHM(OfferProposal offer, com.group2.rms.entity.OfferNegotiation negotiation, String hrNotes);

    /**
     * Báo cho Candidate khi HR TỪ CHỐI yêu cầu thương lượng lương.
     * Thông báo nêu rõ lý do từ chối và hướng dẫn ứng viên xem xét giữ nguyên thư mời ban đầu.
     */
    void sendHrNegotiationRejectedNotificationToCandidate(OfferProposal offer, String hrNotes);

    /**
     * Báo cho Bộ phận Nhân sự (HR) khi Hiring Manager bấm xác nhận tuyển dụng chính thức.
     * HR nhận thông báo để tiến hành thủ tục Onboarding và chuẩn bị hợp đồng lao động.
     */
    void sendHiringConfirmedNotificationToHR(OfferProposal offer);

    /**
     * Báo về cho Ứng viên khi Hiring Manager bấm xác nhận tuyển dụng chính thức.
     * Kết thúc quá trình ứng tuyển của ứng viên, thông báo chúc mừng trúng tuyển chính thức.
     */
    void sendHiringConfirmedNotificationToCandidate(OfferProposal offer);
}

