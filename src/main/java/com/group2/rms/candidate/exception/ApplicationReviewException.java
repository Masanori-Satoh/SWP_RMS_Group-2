package com.group2.rms.candidate.exception;

import com.group2.rms.core.exception.BaseBusinessException;

/**
 * Vi phạm nghiệp vụ khi duyệt hoặc chấm lại hồ sơ (sai bước duyệt, người khác vừa xử lý).
 * {@code field} là ô bị lỗi trên form; {@code null} nghĩa là lỗi chung của form.
 */
public class ApplicationReviewException extends BaseBusinessException {

    private final String field;

    public ApplicationReviewException(String field, String message) {
        super(message, "APPLICATION_REVIEW");
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
