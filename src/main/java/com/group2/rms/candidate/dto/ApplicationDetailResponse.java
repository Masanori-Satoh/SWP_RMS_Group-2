package com.group2.rms.candidate.dto;

import com.group2.rms.core.web.StageItem;
import com.group2.rms.core.web.TimelineItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Chi tiết một hồ sơ cho người tuyển dụng (5.1.23). Chứa điểm AI và nhận xét nội bộ:
 * không bao giờ dùng DTO này cho màn của ứng viên (SRS GBR-02).
 *
 * @param stepper     các bước của thanh tiến độ ({@code stageBar} mode {@code stepper}), bước hiện tại kèm "đang chờ ai"
 * @param interviewId id buổi phỏng vấn mới nhất để mở trang kết quả; {@code null} nếu chưa có buổi nào. Mọi vai trò
 *                    xem được hồ sơ này đều được mở trang kết quả (rule GET {@code /interviews/**})
 * @param viewerRole  tên vai trò người xem, để layout nội bộ chọn đúng sidebar
 */
public record ApplicationDetailResponse(
        Integer applicationId,
        Integer candidateId,
        boolean isPotential,
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
        List<StageItem> stepper,
        Long interviewId,
        Actions actions,
        String viewerRole) {

    /** Thao tác người xem được làm trên hồ sơ này. */
    public record Actions(boolean review, boolean rescreen, boolean scheduleInterview, boolean createOffer,
                          boolean openJobPosting) {
    }
}
