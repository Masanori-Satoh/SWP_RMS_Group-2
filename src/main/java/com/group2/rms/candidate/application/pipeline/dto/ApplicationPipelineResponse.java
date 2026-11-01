package com.group2.rms.candidate.application.pipeline.dto;

import com.group2.rms.candidate.domain.rule.ApplicationProgressResponse;
import com.group2.rms.candidate.domain.rule.ApplicationStatusLabels;

import java.time.LocalDateTime;

/**
 * Một dòng của danh sách hồ sơ (5.1.22).
 *
 * @param ownerLabel ai cầm bóng (đang chờ ai xử lý); {@code null} khi hồ sơ đã kết thúc
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
        String ownerLabel,
        boolean canScheduleInterview,
        boolean canCreateOffer,
        boolean isPotential) {

    /** Dùng trong câu JPQL; nhãn trạng thái chi tiết và quyền thao tác gán sau ở Service bằng {@link #withProgress}. */
    public ApplicationPipelineResponse(Integer applicationId, String fullName, String email, String postingTitle,
                                       LocalDateTime submissionDate, String applicationStatus) {
        this(applicationId, fullName, email, postingTitle, submissionDate, applicationStatus,
                ApplicationStatusLabels.label(applicationStatus), ApplicationStatusLabels.badge(applicationStatus),
                null, false, false, false);
    }

    /** Projection của Talent Pool giữ cờ tiềm năng từ Candidate. */
    public ApplicationPipelineResponse(Integer applicationId, String fullName, String email, String postingTitle,
                                       LocalDateTime submissionDate, String applicationStatus, boolean isPotential) {
        this(applicationId, fullName, email, postingTitle, submissionDate, applicationStatus,
                ApplicationStatusLabels.label(applicationStatus), ApplicationStatusLabels.badge(applicationStatus),
                null, false, false, isPotential);
    }

    /** Nhãn, badge và người cầm bóng theo trạng thái chi tiết, kèm quyền thao tác. */
    public ApplicationPipelineResponse withProgress(ApplicationProgressResponse progress, boolean scheduleInterview,
                                                    boolean createOffer) {
        return new ApplicationPipelineResponse(applicationId, fullName, email, postingTitle, submissionDate,
                applicationStatus, progress.detailLabel(), progress.badge(), progress.ownerLabel(),
                scheduleInterview, createOffer, isPotential);
    }
}
