package com.group2.rms.jobposting.exception;

import com.group2.rms.core.exception.BaseBusinessException;

import java.util.Map;

/*
 * EXCEPTION: lỗi nghiệp vụ của tin; lỗi chung là một câu, lỗi theo ô là map tên ô → câu lỗi.
 */
public class JobPostingValidationException extends BaseBusinessException {

    private final Map<String, String> fieldErrors;

    public JobPostingValidationException(String message) {
        super(message);
        this.fieldErrors = Map.of();
    }

    public JobPostingValidationException(Map<String, String> fieldErrors) {
        super(String.join(" ", fieldErrors.values()));
        this.fieldErrors = fieldErrors;
    }

    /** Lỗi theo ô (rỗng nếu là lỗi chung). */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
