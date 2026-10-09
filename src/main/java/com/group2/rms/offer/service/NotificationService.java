package com.group2.rms.offer.service;

import com.group2.rms.offer.entity.OfferProposal;

public interface NotificationService {

    /**
     * Gửi email/thông báo thư mời nhận việc (Offer Letter) tới ứng viên.
     *
     * @param offer Đề xuất Offer đã được Director duyệt
     */
    void sendOfferLetterToCandidate(OfferProposal offer);

    /**
     * Gửi thông báo tới Giám đốc (Director) khi có đề xuất Offer mới chờ duyệt.
     *
     * @param offer Đề xuất Offer
     */
    void notifyDirectorNewPendingOffer(OfferProposal offer);

    /**
     * Gửi thông báo tới HR (người tạo đề xuất) khi Giám đốc đã quyết định.
     *
     * @param offer Đề xuất Offer
     * @param decision Quyết định (Approved/Rejected)
     * @param comments Nhận xét của Giám đốc
     */
    void notifyHrOfDirectorDecision(OfferProposal offer, String decision, String comments);
}
