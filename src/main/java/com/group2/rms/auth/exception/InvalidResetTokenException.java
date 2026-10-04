package com.group2.rms.auth.exception;

import com.group2.rms.core.exception.BaseBusinessException;
//400 inavlid reset
public class InvalidResetTokenException extends BaseBusinessException {

   public InvalidResetTokenException() {
        super("Invalid or expired reset token.", "INVALID_RESET_TOKEN");
    }

   public InvalidResetTokenException(String message) {
        super(message, "INVALID_RESET_TOKEN");
    }

}
