package com.group2.rms.interview.service;

import com.group2.rms.interview.dto.SubmitFinalResultRequest;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.user.entity.User;

import java.time.LocalDateTime;

/**
 * Hiring Manager chốt kết quả cuối của buổi phỏng vấn (InterviewFinalResult).
 * Kết quả Passed là nguồn của danh sách "ứng viên đạt" khi HR tạo offer.
 */
public interface InterviewFinalResultService {

    /**
     * Ghi kết quả cuối (mỗi lịch một lần, không sửa được). Được chốt kể cả khi hội đồng chưa nộp đủ phiếu.
     * Lịch đang Scheduled/Rescheduled sẽ chuyển sang Completed (GBR-01 chỉ đi tiến).
     *
     * @throws com.group2.rms.core.exception.ResourceNotFoundException không có lịch
     * @throws org.springframework.security.access.AccessDeniedException không phải HM quản lý phòng ban của vị trí
     * @throws com.group2.rms.interview.exception.InterviewFinalResultException đã chốt, lịch đã hủy, chưa diễn ra
     */
    void decide(Long interviewId, SubmitFinalResultRequest request, User hiringManager);

    /**
     * Người dùng có phải Hiring Manager phụ trách phòng ban của vị trí phỏng vấn không.
     * Cần gọi trong transaction (đọc requisition/department LAZY của lịch).
     */
    boolean isDecisionMaker(User user, InterviewSchedule schedule);

    /**
     * Lý do chưa chốt được theo trạng thái/thời gian của lịch; null nếu được chốt.
     */
    static String blockedReason(InterviewSchedule schedule, LocalDateTime now) {
        if (schedule.getInterviewStatus() == null) {
            return "Trạng thái lịch phỏng vấn không hợp lệ.";
        }
        return switch (schedule.getInterviewStatus()) {
            case Cancelled -> "Lịch phỏng vấn đã bị hủy, không thể chốt kết quả.";
            case Completed -> null;
            case Scheduled, Rescheduled -> schedule.getStartTime() != null && now.isBefore(schedule.getStartTime())
                    ? "Buổi phỏng vấn chưa diễn ra, chưa thể chốt kết quả."
                    : null;
        };
    }
}
