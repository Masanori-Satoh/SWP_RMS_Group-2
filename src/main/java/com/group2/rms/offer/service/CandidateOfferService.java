package com.group2.rms.offer.service;

import com.group2.rms.offer.dto.CandidateOfferResponse;

import java.util.List;

/**
 * Thư mời nhận việc của chính ứng viên đang đăng nhập: xem và trả lời (nhận / từ chối).
 */
public interface CandidateOfferService {

    /** Các offer đã gửi cho ứng viên (chờ trả lời, đã nhận, đã từ chối), mới nhất trước. */
    List<CandidateOfferResponse> list();

    /**
     * Ứng viên nhận offer: offer thành {@code Accepted}, hồ sơ thành {@code Hired}.
     *
     * @return tên vị trí, cho thông báo thành công
     */
    String accept(Integer offerId);

    /**
     * Ứng viên từ chối offer: offer thành {@code Declined}, hồ sơ thành {@code Rejected}.
     *
     * @return tên vị trí, cho thông báo thành công
     */
    String decline(Integer offerId);
}
