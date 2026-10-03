package com.group2.rms.service;

import com.group2.rms.entity.OfferProposal;

public interface NotificationService {

    /**
     * Gửi email/thông báo thư mời nhận việc (Offer Letter) tới ứng viên.
     *
     * @param offer Đề xuất Offer đã được Director duyệt
     */
    void sendOfferLetterToCandidate(OfferProposal offer);
}
