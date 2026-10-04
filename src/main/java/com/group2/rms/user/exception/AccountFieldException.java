package com.group2.rms.user.exception;

import com.group2.rms.core.exception.BaseBusinessException;

//500
public class AccountFieldException extends BaseBusinessException {

    private final String field;

    public AccountFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
