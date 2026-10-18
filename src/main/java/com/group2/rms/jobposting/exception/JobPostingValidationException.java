package com.group2.rms.jobposting.exception;

import com.group2.rms.core.exception.BaseBusinessException;

import java.util.Map;

/**
 * Lỗi nghiệp vụ của tin tuyển dụng.
 * - Lỗi chung (không gắn với ô nào): dùng constructor nhận một câu, ví dụ "Chỉ tin nháp mới được xóa."
 * - Lỗi theo ô của form: dùng constructor nhận map tên ô → câu lỗi, controller đưa từng lỗi về cạnh ô đó.
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
