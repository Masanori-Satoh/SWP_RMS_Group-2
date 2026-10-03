package com.group2.rms.user.exception;

import com.group2.rms.core.exception.BaseBusinessException;

public class AccountConflictException extends BaseBusinessException{

    public AccountConflictException(String message) {
        // dupplicate unique constraint
        super(message,"ACCOUNT_CONFLICT");

    }

}
