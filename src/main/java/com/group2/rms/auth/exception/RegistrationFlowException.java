package com.group2.rms.auth.exception;

import com.group2.rms.core.exception.BaseBusinessException;

public class RegistrationFlowException extends BaseBusinessException {
    public enum Step { REGISTER, OTP }

    private final Step step;
    private final String field;
    private final long retryAfterSeconds;
    private final RegistrationDetails details;

    public RegistrationFlowException(Step step, String field, String errorCode, String message) {
        this(step, field, errorCode, message, 0, null);
    }

    public RegistrationFlowException(Step step, String field, String errorCode, String message,
            long retryAfterSeconds, RegistrationDetails details) {
        super(message, errorCode);
        this.step = step;
        this.field = field;
        this.retryAfterSeconds = retryAfterSeconds;
        this.details = details;
    }

    public Step getStep() { return step; }
    public String getField() { return field; }
    public long getRetryAfterSeconds() { return retryAfterSeconds; }
    public RegistrationDetails getDetails() { return details; }

    /** Only safe form fields may be carried to the global handler. */
    public record RegistrationDetails(String fullName, String username, String email) {
        @Override
        public String toString() { return "RegistrationDetails[redacted]"; }
    }
}
