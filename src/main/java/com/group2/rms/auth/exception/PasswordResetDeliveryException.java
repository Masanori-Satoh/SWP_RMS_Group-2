package com.group2.rms.auth.exception;

import com.group2.rms.core.exception.BaseBusinessException;
//302
public class PasswordResetDeliveryException extends BaseBusinessException {

    public PasswordResetDeliveryException() {
        super("Password recovery email delivery failed.", "MAIL_DELIVERY_FAILED");
    }

}
