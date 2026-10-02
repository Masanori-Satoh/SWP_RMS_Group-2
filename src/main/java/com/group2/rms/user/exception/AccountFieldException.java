package com.group2.rms.user.exception;

public class AccountFieldException extends RuntimeException {

    private final String field;

    public AccountFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
