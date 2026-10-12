package com.group2.rms.candidate.dto;

import com.group2.rms.candidate.service.ApplicationStatusLabels;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một dòng của danh sách hồ sơ (5.1.22).
 *
 * @param aiMatchScore điểm AI mới nhất; {@code null} nếu chưa được chấm
 */
public record ApplicationPipelineResponse(
        Integer applicationId,
        String fullName,
        String email,
        String postingTitle,
        LocalDateTime submissionDate,
        String applicationStatus,
        String statusLabel,
        String statusBadge,
        BigDecimal aiMatchScore,
        boolean canScheduleInterview,
        boolean canCreateOffer) {

    /** Dùng trong câu JPQL; quyền thao tác gán sau ở Service bằng {@link #withActions}. */
    public ApplicationPipelineResponse(Integer applicationId, String fullName, String email, String postingTitle,
                                       LocalDateTime submissionDate, String applicationStatus, BigDecimal aiMatchScore) {
        this(applicationId, fullName, email, postingTitle, submissionDate, applicationStatus,
                ApplicationStatusLabels.label(applicationStatus), ApplicationStatusLabels.badge(applicationStatus),
                aiMatchScore, false, false);
    }

    public ApplicationPipelineResponse withActions(boolean scheduleInterview, boolean createOffer) {
        return new ApplicationPipelineResponse(applicationId, fullName, email, postingTitle, submissionDate,
                applicationStatus, statusLabel, statusBadge, aiMatchScore, scheduleInterview, createOffer);
    }
}
