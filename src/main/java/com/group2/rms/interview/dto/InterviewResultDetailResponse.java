package com.group2.rms.interview.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dữ liệu trang Interview Result Details (SRS 5.1.28).
 *
 * canSeeAll = false: người xem là Interviewer và chưa có Final Result (GBR-05), nên
 * evaluations chỉ chứa phiếu của chính họ, averages = null, recommendations rỗng.
 * Các hạn (evaluationDeadline, decisionDeadline, considerDeadline) chỉ để cảnh báo, xem InterviewDeadlines.
 */
public record InterviewResultDetailResponse(
        Long interviewId,
        Integer applicationId,
        String candidateName,
        String candidateEmail,
        String jobTitle,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String formatDisplay,
        String locationOrLink,
        String statusDisplay,
        String statusBadge,
        List<PanelMemberStatus> panel,
        int submittedCount,
        List<InterviewEvaluationResponse> evaluations,
        Averages averages,
        List<RecommendationCount> recommendations,
        FinalResult finalResult,
        boolean canSeeAll,
        boolean canEvaluate,
        String evaluateBlockedReason,
        boolean canFinalize,
        String finalizeBlockedReason,
        LocalDateTime evaluationDeadline,
        LocalDateTime decisionDeadline,
        boolean decisionOverdue
) {

    /** Một thành viên hội đồng, cờ đã nộp phiếu và trạng thái chấm điểm (Chờ / Đã nộp / Nộp trễ / Quá hạn). */
    public record PanelMemberStatus(
            Integer interviewerId,
            String fullName,
            String roleInPanelDisplay,
            boolean submitted,
            boolean me,
            String progressDisplay,
            String progressBadge
    ) {
    }

    /** Điểm trung bình từng tiêu chí trên các phiếu đã nộp, làm tròn 1 chữ số. */
    public record Averages(
            Double technical,
            Double softSkill,
            Double culturalFit,
            Double overall
    ) {
    }

    /** Phân bố đề xuất (Hire / Consider / No_Hire). */
    public record RecommendationCount(
            String value,
            String display,
            String badge,
            long count
    ) {
    }

    /**
     * Quyết định của Hiring Manager (bảng InterviewFinalResult), chỉ có khi đã được ghi.
     * considering = true: quyết định tạm, còn chốt lại được; summaryText là bản gốc để điền lại form.
     */
    public record FinalResult(
            String decision,
            String decisionDisplay,
            String decisionBadge,
            String hiringManagerName,
            BigDecimal recommendedSalary,
            String summaryHtml,
            LocalDateTime approvedAt,
            boolean considering,
            LocalDateTime consideredAt,
            LocalDateTime considerDeadline,
            boolean considerOverdue,
            String summaryText
    ) {
    }
}
