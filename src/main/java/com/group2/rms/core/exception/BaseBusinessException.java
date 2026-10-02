package com.group2.rms.core.exception;

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
