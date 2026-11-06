package com.group2.rms.user.exception;

import com.group2.rms.core.exception.BaseBusinessException;

/** The selected account can no longer be managed from this account list. */
public class AccountOperationException extends BaseBusinessException {
    public AccountOperationException(String message) {
        super(message, "ACCOUNT_OPERATION_REJECTED");
    }
    public AccountOperationException(String message, String code) {
        super(message, code);
    }
}

