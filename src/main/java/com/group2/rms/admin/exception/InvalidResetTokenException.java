package com.group2.rms.admin.exception;

import com.group2.rms.core.exception.BaseBusinessException;

public class InvalidResetTokenException extends BaseBusinessException {

   public InvalidResetTokenException(String message) {
        super(message, "INVALID_RESET_TOKEN");
    }

}
