package com.group2.rms.jobposting.exception;

import com.group2.rms.core.exception.BaseBusinessException;

public class JobPostingValidationException extends BaseBusinessException {
    public JobPostingValidationException(String message) {
        super(message);
    }
}
