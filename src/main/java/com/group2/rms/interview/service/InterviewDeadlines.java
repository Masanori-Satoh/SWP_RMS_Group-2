package com.group2.rms.interview.service;

import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.interview.entity.InterviewStatus;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Các mốc thời gian của vòng phỏng vấn (một nguồn duy nhất cho service, giao diện và test).
 * <pre>
 *   Hạn chấm điểm  = EndTime + 24h      (quá hạn vẫn nộp được, phiếu bị đánh dấu "Nộp trễ")
 *   Hạn chốt       = hạn chấm điểm + 2 ngày
 *   Hạn cân nhắc   = ConsideredAt + 7 ngày
 * </pre>
 * Mọi hạn chỉ dùng để cảnh báo, không khóa thao tác; riêng trong 24h đầu HM phải chờ đủ phiếu mới được chốt.
 * Lịch đã hủy không có hạn nào.
 */
public final class InterviewDeadlines {

    public static final Duration EVALUATION_WINDOW = Duration.ofHours(24);
    public static final Duration DECISION_WINDOW = Duration.ofDays(2);
    public static final Duration CONSIDER_WINDOW = Duration.ofDays(7);

    private InterviewDeadlines() {
    }

    public static LocalDateTime evaluationDeadline(InterviewSchedule schedule) {
        return schedule == null ? null : evaluationDeadline(schedule.getInterviewStatus(), schedule.getEndTime());
    }

    public static LocalDateTime evaluationDeadline(InterviewStatus status, LocalDateTime endTime) {
        if (endTime == null || status == InterviewStatus.Cancelled) {
            return null;
        }
        return endTime.plus(EVALUATION_WINDOW);
    }

    public static LocalDateTime decisionDeadline(InterviewSchedule schedule) {
        return schedule == null ? null : decisionDeadline(schedule.getInterviewStatus(), schedule.getEndTime());
    }

    public static LocalDateTime decisionDeadline(InterviewStatus status, LocalDateTime endTime) {
        /*
         * Hạn chốt nối tiếp hạn chấm: EndTime + 24h + 2 ngày, không phải EndTime + 2 ngày.
         */
        LocalDateTime evaluationDeadline = evaluationDeadline(status, endTime);
        return evaluationDeadline != null ? evaluationDeadline.plus(DECISION_WINDOW) : null;
    }

    public static LocalDateTime considerDeadline(LocalDateTime consideredAt) {
        /*
         * Tính từ lần Considering đầu tiên; lưu lại quyết định không gia hạn thêm 7 ngày.
         */
        return consideredAt != null ? consideredAt.plus(CONSIDER_WINDOW) : null;
    }

    /** Phiếu nộp sau hạn chấm điểm. */
    public static boolean isLate(InterviewSchedule schedule, LocalDateTime evaluatedAt) {
        /*
         * Nộp đúng deadline chưa bị gắn cờ trễ; chỉ thời điểm sau deadline mới tính trễ.
         */
        LocalDateTime deadline = evaluationDeadline(schedule);
        return deadline != null && evaluatedAt != null && evaluatedAt.isAfter(deadline);
    }

    /** Còn trong 24h chấm điểm (HM phải chờ đủ phiếu). */
    public static boolean withinEvaluationWindow(InterviewSchedule schedule, LocalDateTime now) {
        /*
         * Đúng deadline đã hết cửa sổ bắt buộc chờ đủ phiếu, khác điều kiện đánh dấu một phiếu nộp trễ.
         */
        LocalDateTime deadline = evaluationDeadline(schedule);
        return deadline != null && now.isBefore(deadline);
    }

    /** Trạng thái chấm điểm của một thành viên hội đồng; submittedAt = null nếu chưa nộp. */
    public static EvaluationProgress progress(InterviewSchedule schedule, LocalDateTime submittedAt, LocalDateTime now) {
        return progress(schedule.getInterviewStatus(), schedule.getStartTime(), schedule.getEndTime(), submittedAt, now);
    }

    public static EvaluationProgress progress(InterviewStatus status, LocalDateTime startTime, LocalDateTime endTime,
                                              LocalDateTime submittedAt, LocalDateTime now) {
        /*
         * Phiếu đã nộp giữ nhãn thực tế dù lịch bị hủy sau đó;
         * các trạng thái hủy/chưa tới giờ/quá hạn còn lại dành cho người chưa nộp.
         */
        LocalDateTime deadline = evaluationDeadline(status, endTime);
        if (submittedAt != null) {
            return deadline != null && submittedAt.isAfter(deadline)
                    ? EvaluationProgress.SUBMITTED_LATE : EvaluationProgress.SUBMITTED;
        }
        if (status == InterviewStatus.Cancelled) {
            return EvaluationProgress.CANCELLED;
        }
        if (startTime != null && now.isBefore(startTime)) {
            return EvaluationProgress.NOT_STARTED;
        }
        /* Đúng mốc deadline đã hết thời gian chờ phiếu; nhãn này chỉ cảnh báo, không quyết định quyền nộp. */
        return deadline == null || now.isBefore(deadline) ? EvaluationProgress.PENDING : EvaluationProgress.OVERDUE;
    }

    /** Trạng thái chấm điểm hiển thị trên giao diện (suy ra, không lưu DB). */
    public enum EvaluationProgress {
        NOT_STARTED("Chưa đến giờ", "badge--neutral"),
        PENDING("Chờ đánh giá", "badge--warning"),
        SUBMITTED("Đã nộp", "badge--success"),
        SUBMITTED_LATE("Nộp trễ", "badge--warning"),
        OVERDUE("Quá hạn đánh giá", "badge--danger"),
        CANCELLED("Lịch đã hủy", "badge--neutral");

        private final String displayName;
        private final String badgeClass;

        EvaluationProgress(String displayName, String badgeClass) {
            this.displayName = displayName;
            this.badgeClass = badgeClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getBadgeClass() {
            return badgeClass;
        }
    }
}
