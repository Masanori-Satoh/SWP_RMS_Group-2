package com.group2.rms.interview;

import lombok.Getter;

/**
 * Custom RuntimeException cho các lỗi sai phạm luồng nghiệp vụ trong module Interview:
 * - Rule GBR-01: Chuyển đổi trạng thái phỏng vấn không hợp lệ (đi lùi hoặc vi phạm state machine).
 * - Rule GBR-05: Cố tình truy cập lịch phỏng vấn không được phân công.
 * - Phân quyền hội đồng: Cố tình gán HR vào Hội đồng phỏng vấn.
 */
@Getter
public class InterviewStatusException extends RuntimeException {

    private final InterviewStatus currentStatus;
    private final InterviewStatus targetStatus;

    public InterviewStatusException(String message) {
        super(message);
        this.currentStatus = null;
        this.targetStatus = null;
    }

    public InterviewStatusException(String message, Throwable cause) {
        super(message, cause);
        this.currentStatus = null;
        this.targetStatus = null;
    }

    public InterviewStatusException(InterviewStatus currentStatus, InterviewStatus targetStatus, String message) {
        super(message);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }
}
