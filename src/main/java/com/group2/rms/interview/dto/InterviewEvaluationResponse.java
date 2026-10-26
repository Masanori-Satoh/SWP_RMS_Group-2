package com.group2.rms.interview.dto;

import java.time.LocalDateTime;

/**
 * Một phiếu đánh giá trên trang Interview Result Details (SRS 5.1.28).
 * strengthsHtml / weaknessesHtml / commentsHtml đã escape HTML và đổi xuống dòng thành &lt;br/&gt; ở Service
 * (render bằng th:utext).
 * late = true: phiếu nộp sau hạn 24h kể từ giờ kết thúc buổi phỏng vấn.
 */
public record InterviewEvaluationResponse(
        Integer evaluationId,
        Integer interviewerId,
        String interviewerName,
        String roleInPanelDisplay,
        Integer technicalScore,
        Integer softSkillScore,
        Integer culturalFitScore,
        Double averageScore,
        String recommendation,
        String recommendationDisplay,
        String recommendationBadge,
        String strengthsHtml,
        String weaknessesHtml,
        String commentsHtml,
        LocalDateTime evaluatedAt,
        boolean mine,
        boolean late
) {
}
