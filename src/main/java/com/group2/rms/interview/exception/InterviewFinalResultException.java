package com.group2.rms.interview.exception;

import com.group2.rms.core.exception.BaseBusinessException;

/**
 * Vi phạm nghiệp vụ khi Hiring Manager chốt kết quả phỏng vấn:
 * đã chốt rồi, lịch đã hủy, buổi phỏng vấn chưa diễn ra.
 * {@code field} khác null khi lỗi gắn với một ô cụ thể của form.
 */
public class InterviewFinalResultException extends BaseBusinessException {

    private final String field;

    public InterviewFinalResultException(String message) {
        this(null, message);
    }

    public InterviewFinalResultException(String field, String message) {
        super(message, "INTERVIEW_FINAL_RESULT_ERROR");
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
