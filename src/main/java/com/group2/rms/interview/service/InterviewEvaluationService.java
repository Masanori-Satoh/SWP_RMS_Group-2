package com.group2.rms.interview.service;

import com.group2.rms.interview.dto.SubmitEvaluationRequest;
import com.group2.rms.interview.entity.InterviewSchedule;

import java.time.LocalDateTime;

/**
 * Nộp phiếu đánh giá phỏng vấn (Submit Interview Evaluate - SRS 5.1.29).
 */
public interface InterviewEvaluationService {

    /**
     * Thành viên hội đồng nộp phiếu cho một lịch phỏng vấn.
     * Phiếu đầu tiên của lịch đang Scheduled/Rescheduled sẽ chuyển lịch sang Completed (D3').
     *
     * @throws com.group2.rms.core.exception.ResourceNotFoundException không có lịch
     * @throws org.springframework.security.access.AccessDeniedException người nộp không thuộc hội đồng (GBR-05)
     * @throws com.group2.rms.interview.exception.InterviewEvaluationException đã nộp, lịch đã hủy, chưa tới giờ
     */
    void submit(Long interviewId, SubmitEvaluationRequest request, Integer evaluatorId);

    /**
     * Lý do chưa được nộp phiếu theo trạng thái/thời gian của lịch; null nếu được nộp.
     * Dùng chung cho Service nộp phiếu và trang kết quả (ẩn/hiện nút "Đánh giá").
     */
    static String blockedReason(InterviewSchedule schedule, LocalDateTime now) {
        if (schedule.getInterviewStatus() == null) {
            return "Trạng thái lịch phỏng vấn không hợp lệ.";
        }
        return switch (schedule.getInterviewStatus()) {
            case Cancelled -> "Lịch phỏng vấn đã bị hủy, không thể đánh giá.";
            case Completed -> null;
            case Scheduled, Rescheduled -> schedule.getStartTime() != null && now.isBefore(schedule.getStartTime())
                    ? "Chưa đến giờ phỏng vấn, chỉ đánh giá được sau khi buổi phỏng vấn bắt đầu."
                    : null;
        };
    }
}
