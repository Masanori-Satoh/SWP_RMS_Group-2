package com.group2.rms.interview.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dữ liệu trang Interview Result Details (SRS 5.1.28).
 *
 * canSeeAll = false: người xem là Interviewer và chưa có Final Result (GBR-05), nên
 * evaluations chỉ chứa phiếu của chính họ, averages = null, recommendations rỗng.
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
        String finalizeBlockedReason
) {

    /** Một thành viên hội đồng và cờ đã nộp phiếu hay chưa. */
    public record PanelMemberStatus(
            Integer interviewerId,
            String fullName,
            String roleInPanelDisplay,
            boolean submitted,
            boolean me
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

    /** Quyết định cuối của Hiring Manager (bảng InterviewFinalResult), chỉ có khi đã được ghi. */
    public record FinalResult(
            String decision,
            String decisionDisplay,
            String decisionBadge,
            String hiringManagerName,
            BigDecimal recommendedSalary,
            String summaryHtml,
            LocalDateTime approvedAt
    ) {
    }
}
