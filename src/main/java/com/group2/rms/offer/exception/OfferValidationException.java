package com.group2.rms.offer.exception;

import com.group2.rms.core.exception.BaseBusinessException;

/**
 * Custom Exception cho nghiệp vụ Offer theo chuẩn ARCHITECTURE_GUIDE.md.
 * Kế thừa BaseBusinessException để GlobalExceptionHandler tự động map về HTTP 400 Bad Request.
 */
public class OfferValidationException extends BaseBusinessException {

    public OfferValidationException(String message) {
        super(message, "OFFER_VALIDATION_ERROR");
    }

    public OfferValidationException(String message, String errorCode) {
        super(message, errorCode);
    }
}
