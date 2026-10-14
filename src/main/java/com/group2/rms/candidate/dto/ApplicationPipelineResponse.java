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
                boolean canCreateOffer,
                boolean isPotential) {

        /**
         * Dùng trong câu JPQL; nhãn trạng thái chi tiết và quyền thao tác gán sau ở
         * Service bằng {@link #withProgress}.
         */
        // get data belong to form in db
        public ApplicationPipelineResponse(Integer applicationId, String fullName, String email, String postingTitle,
                        LocalDateTime submissionDate, String applicationStatus, BigDecimal aiMatchScore) {
                this(applicationId, fullName, email, postingTitle, submissionDate, applicationStatus, aiMatchScore,
                                false);
        }

        // update status label and badge(for fe), interview, offer i proces by service
        // layer
        public ApplicationPipelineResponse(Integer applicationId, String fullName, String email, String postingTitle,
                        LocalDateTime submissionDate, String applicationStatus, BigDecimal aiMatchScore,
                        boolean potential) {
                this(applicationId, fullName, email, postingTitle, submissionDate, applicationStatus,
                                ApplicationStatusLabels.label(applicationStatus),
                                ApplicationStatusLabels.badge(applicationStatus),
                                aiMatchScore, false, false, potential);
        }

        /**
         * Nhãn và badge theo trạng thái chi tiết (hồ sơ đang chờ ai), kèm quyền thao
         * tác.
         */
        public ApplicationPipelineResponse withProgress(ApplicationProgressResponse progress, boolean scheduleInterview,
                        boolean createOffer) {
                return new ApplicationPipelineResponse(applicationId, fullName, email, postingTitle, submissionDate,
                                applicationStatus, progress.detailLabel(), progress.badge(), aiMatchScore,
                                scheduleInterview,
                                createOffer, this.isPotential);
        }
}
