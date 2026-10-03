package com.group2.rms.admin.exception;

import com.group2.rms.core.exception.BaseBusinessException;

public class PasswordResetDeliveryException extends BaseBusinessException {

    public PasswordResetDeliveryException() {
        super("Password recovery email delivery failed.", "MAIL_DELIVERY_FAILED");
    }

}
