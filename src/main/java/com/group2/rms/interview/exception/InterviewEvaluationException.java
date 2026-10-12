package com.group2.rms.interview.exception;

import com.group2.rms.core.exception.BaseBusinessException;

/**
 * Vi phạm nghiệp vụ khi nộp phiếu đánh giá phỏng vấn:
 * đã nộp rồi, lịch đã hủy, chưa tới giờ phỏng vấn.
 * {@code field} khác null khi lỗi gắn với một ô cụ thể của form.
 */
public class InterviewEvaluationException extends BaseBusinessException {

    private final String field;

    public InterviewEvaluationException(String message) {
        this(null, message);
    }

    public InterviewEvaluationException(String field, String message) {
        super(message, "INTERVIEW_EVALUATION_ERROR");
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
