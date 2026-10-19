package com.group2.rms.jobposting.exception;

import com.group2.rms.core.exception.BaseBusinessException;

import java.util.Map;

/**
 * Lỗi nghiệp vụ của tin tuyển dụng.
 */
public class JobPostingValidationException extends BaseBusinessException {

    private final Map<String, String> fieldErrors;

    /** Lỗi chung, không gắn với ô nào. */
    public JobPostingValidationException(String message) {
        super(message);
        this.fieldErrors = Map.of();
    }

    /** Lỗi theo ô: map tên ô → câu lỗi, controller đưa từng lỗi về cạnh ô đó. */
    public JobPostingValidationException(Map<String, String> fieldErrors) {
        super(String.join(" ", fieldErrors.values()));
        this.fieldErrors = fieldErrors;
    }

    /** Lỗi theo ô (rỗng nếu là lỗi chung). */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
