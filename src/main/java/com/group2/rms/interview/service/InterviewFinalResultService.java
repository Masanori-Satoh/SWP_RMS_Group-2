package com.group2.rms.interview.service;

import com.group2.rms.interview.dto.SubmitFinalResultRequest;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.user.entity.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Hiring Manager (hoặc Giám đốc khi tuyển Hiring Manager) chốt kết quả cuối của buổi phỏng vấn (InterviewFinalResult).
 * Kết quả Passed là nguồn của danh sách "ứng viên đạt" khi HR tạo offer.
 * Considering là quyết định tạm: hồ sơ giữ Interviewing, người chốt chốt lại Passed/Failed sau.
 */
public interface InterviewFinalResultService {

    DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    /**
     * Ghi kết quả cuối. Chưa có kết quả: tạo mới (Considering thì ghi ConsideredAt).
     * Đang Considering: chốt lại được (Passed / Failed / vẫn Considering), giữ nguyên ConsideredAt.
     * Đã Passed / Failed: không sửa được.
     * Trong 24h sau giờ kết thúc phải đủ phiếu của hội đồng; sau đó chốt tự do.
     * Lịch đang Scheduled/Rescheduled sẽ chuyển sang Completed (GBR-01 chỉ đi tiến).
     *
     * @throws com.group2.rms.core.exception.ResourceNotFoundException không có lịch
     * @throws org.springframework.security.access.AccessDeniedException không phải HM quản lý phòng ban của vị trí
     * @throws com.group2.rms.interview.exception.InterviewFinalResultException đã chốt, lịch đã hủy, chưa diễn ra,
     *                                                                         chưa đủ phiếu trong 24h
     */
    void decide(Long interviewId, SubmitFinalResultRequest request, User hiringManager);

    /**
     * Vị trí phỏng vấn có yêu cầu Giám đốc chốt kết quả hay không.
     * Áp dụng khi tuyển vị trí Hiring Manager (Phương án 2: yêu cầu do Giám đốc tạo).
     */
    boolean requiresDirectorDecision(InterviewSchedule schedule);

    /**
     * Người dùng có phải người có quyền chốt kết quả của vị trí phỏng vấn không
     * (Hiring Manager phụ trách phòng ban cho vị trí thường; Giám đốc khi tuyển Hiring Manager).
     * Cần gọi trong transaction (đọc requisition/department LAZY của lịch).
     */
    boolean isDecisionMaker(User user, InterviewSchedule schedule);

    /**
     * Lý do chưa chốt được theo trạng thái/thời gian của lịch và số phiếu; null nếu được chốt.
     *
     * @param submittedCount số thành viên hội đồng đã nộp phiếu
     * @param panelSize      số thành viên hội đồng
     */
    static String blockedReason(InterviewSchedule schedule, int submittedCount, int panelSize, LocalDateTime now) {
        if (schedule.getInterviewStatus() == null) {
            return "Trạng thái lịch phỏng vấn không hợp lệ.";
        }
        String statusReason = switch (schedule.getInterviewStatus()) {
            case Cancelled -> "Lịch phỏng vấn đã bị hủy, không thể chốt kết quả.";
            case Completed -> null;
            case Scheduled, Rescheduled -> schedule.getStartTime() != null && now.isBefore(schedule.getStartTime())
                    ? "Buổi phỏng vấn chưa diễn ra, chưa thể chốt kết quả."
                    : null;
        };
        if (statusReason != null) {
            return statusReason;
        }
        if (submittedCount < panelSize && InterviewDeadlines.withinEvaluationWindow(schedule, now)) {
            return "Chưa đủ phiếu đánh giá (" + submittedCount + "/" + panelSize + "). Có thể chốt khi đủ phiếu hoặc sau "
                    + InterviewDeadlines.evaluationDeadline(schedule).format(DEADLINE_FORMAT) + ".";
        }
        return null;
    }
}
