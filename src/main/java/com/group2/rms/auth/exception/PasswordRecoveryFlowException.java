package com.group2.rms.auth.exception;

import com.group2.rms.core.exception.BaseBusinessException;

public class PasswordRecoveryFlowException extends BaseBusinessException {
    // step occur error
    public enum Step {
        FORGOT,
        OTP,
        PASSWORD
    }

    private final Step step; // step that occur error
    private final String field; // field that occur error
    private final long retryAfterSeconds; // time to retry after error
    // constructor for normal error

    public PasswordRecoveryFlowException(
            Step step,
            String field,
            String errorCode,
            String message) {
        this(step, field, errorCode, message, 0);
    }

    // constructor for system error(such as rate limit)
    public PasswordRecoveryFlowException(
            Step step,
            String field,
            String errorCode,
            String message,
            long retryAfterSeconds) {

        super(message, errorCode);
        this.step = step;
        this.field = field;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Step getStep() {
        return step;
    }

    public String getField() {
        return field;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
