package com.group2.rms.admin.exception;

import com.group2.rms.core.exception.BaseBusinessException;

public class PasswordRecoveryUnavailableException extends BaseBusinessException {

    public PasswordRecoveryUnavailableException() {
        // 503: haven't configured yet
        super("Password recovery email is not configured yet.", "MAIL_UNAVAILABLE");
    }

}
