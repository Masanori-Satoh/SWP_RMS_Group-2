package com.group2.rms.candidate.dto;

import com.group2.rms.core.web.TimelineItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Chi tiết một hồ sơ cho người tuyển dụng (5.1.23). Chứa điểm AI và nhận xét nội bộ:
 * không bao giờ dùng DTO này cho màn của ứng viên (SRS GBR-02).
 *
 * @param viewerRole tên vai trò người xem, để layout nội bộ chọn đúng sidebar
 */
public record ApplicationDetailResponse(
        Integer applicationId,
        String fullName,
        String email,
        String phoneNumber,
        String linkedInUrl,
        String portfolioUrl,
        Integer requisitionId,
        String requisitionCode,
        Integer jobPostingId,
        String postingTitle,
        String departmentName,
        LocalDateTime submissionDate,
        String applicationStatus,
        String statusLabel,
        String statusBadge,
        BigDecimal aiMatchScore,
        LocalDateTime screenedAt,
        List<TimelineItem> timeline,
        Actions actions,
        String viewerRole) {

    /** Thao tác người xem được làm trên hồ sơ này. */
    public record Actions(boolean review, boolean rescreen, boolean scheduleInterview, boolean createOffer,
                          boolean openJobPosting) {
    }
}
