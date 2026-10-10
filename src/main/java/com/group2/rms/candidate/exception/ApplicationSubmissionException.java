package com.group2.rms.candidate.exception;

import com.group2.rms.core.exception.BaseBusinessException;

/**
 * Vi phạm nghiệp vụ khi nộp đơn ứng tuyển (tin hết hạn, nộp trùng, file CV không hợp lệ).
 * {@code field} là ô bị lỗi trên form; {@code null} nghĩa là lỗi chung của form.
 */
public class ApplicationSubmissionException extends BaseBusinessException {

    private final String field;

    public ApplicationSubmissionException(String field, String message) {
        super(message, "APPLICATION_SUBMISSION");
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
