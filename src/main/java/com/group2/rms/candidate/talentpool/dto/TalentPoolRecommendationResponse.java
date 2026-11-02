package com.group2.rms.candidate.talentpool.dto;

import java.time.LocalDateTime;

/**
 * UI state derived from persisted draft, origin and structured review
 * decisions.
 */
// name Hr recommend Date, current status candecide
public record TalentPoolRecommendationResponse(Integer applicationId, Integer candidateId,
        Integer jobPostingId, String jobTitle, String fullName, String recommendedBy,
        LocalDateTime recommendedAt, String statusLabel, boolean pending, boolean canDecide,
        String email, String phoneNumber, String stateKey) {
    public TalentPoolRecommendationResponse(Integer applicationId, Integer candidateId,
            Integer jobPostingId, String jobTitle, String fullName, String recommendedBy,
            LocalDateTime recommendedAt, String statusLabel, boolean pending, boolean canDecide) {
        this(applicationId, candidateId, jobPostingId, jobTitle, fullName, recommendedBy,
                recommendedAt, statusLabel, pending, canDecide, null, null,
                pending ? "pending"
                        : "HM đã chấp nhận".equals(statusLabel) ? "accepted"
                                : "HM đã từ chối".equals(statusLabel) ? "rejected" : "closed");
    }

    public String statusTone() {
        return switch (stateKey) {
            case "pending" -> "warning";
            case "accepted" -> "success";
            case "rejected", "closed" -> "neutral";
            default -> "neutral";
        };
    }

    public String statusHint() {
        return switch (stateKey) {
            case "pending" -> "Đang chờ HM phụ trách phản hồi.";
            case "accepted" -> "HM đã đồng ý; HR tiếp tục liên hệ ứng viên.";
            case "rejected" -> "HM không tiếp tục gợi ý này.";
            default -> "Gợi ý đã kết thúc chờ phản hồi.";
        };
    }
}
