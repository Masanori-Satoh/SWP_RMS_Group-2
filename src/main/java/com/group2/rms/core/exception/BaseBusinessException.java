package com.group2.rms.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Base exception for all business logic errors.
 * Mapped to HTTP 400 Bad Request by default instead of 500 Server Error.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
//400 core
public class BaseBusinessException extends RuntimeException {
    private final String errorCode;

    public BaseBusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    public BaseBusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
