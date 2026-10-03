package com.group2.rms.offer.service;

import com.group2.rms.offer.entity.OfferProposal;

public interface NotificationService {

    /**
     * Gửi email/thông báo thư mời nhận việc (Offer Letter) tới ứng viên.
     *
     * @param offer Đề xuất Offer đã được Director duyệt
     */
    void sendOfferLetterToCandidate(OfferProposal offer);
}
