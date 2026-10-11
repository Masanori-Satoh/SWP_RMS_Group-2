package com.group2.rms.interview.entity;

import lombok.Getter;

/**
 * Đề xuất của người phỏng vấn trên phiếu đánh giá.
 * Tên hằng trùng đúng giá trị trong DB:
 * CK_InterviewEvaluation_Recommendation: CHECK (Recommendation IN (N'Hire', N'No_Hire', N'Consider'))
 * Entity InterviewEvaluation vẫn lưu String; enum này chỉ dùng để kiểm tra và hiển thị.
 */
@Getter
public enum EvaluationRecommendation {
    Hire("Nên tuyển", "badge--success"),
    Consider("Cân nhắc", "badge--warning"),
    No_Hire("Không tuyển", "badge--danger");

    private final String displayName;
    private final String badgeClass;

    EvaluationRecommendation(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    /**
     * Đổi giá trị DB sang enum; giá trị lạ trả về null thay vì ném lỗi.
     */
    public static EvaluationRecommendation fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (EvaluationRecommendation recommendation : values()) {
            if (recommendation.name().equals(value)) {
                return recommendation;
            }
        }
        return null;
    }
}
