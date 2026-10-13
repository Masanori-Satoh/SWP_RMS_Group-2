package com.group2.rms.offer.exception;

import com.group2.rms.core.exception.BaseBusinessException;

/**
 * Ứng viên không trả lời được offer: offer không còn ở trạng thái chờ trả lời, hoặc vừa được cập nhật đúng lúc bấm.
 */
public class OfferResponseException extends BaseBusinessException {

    public OfferResponseException(String message) {
        super(message, "OFFER_RESPONSE");
    }
}
