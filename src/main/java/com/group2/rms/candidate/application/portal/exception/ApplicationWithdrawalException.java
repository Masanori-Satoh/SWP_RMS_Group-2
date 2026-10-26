package com.group2.rms.candidate.application.portal.exception;

import com.group2.rms.core.exception.BaseBusinessException;

/**
 * Ứng viên không rút được đơn: đơn đã qua vòng sơ tuyển, hoặc HR vừa xử lý đúng lúc ứng viên bấm rút.
 */
public class ApplicationWithdrawalException extends BaseBusinessException {

    public ApplicationWithdrawalException(String message) {
        super(message, "APPLICATION_WITHDRAWAL");
    }
}
