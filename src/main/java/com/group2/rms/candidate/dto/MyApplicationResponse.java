package com.group2.rms.candidate.dto;

import java.time.LocalDateTime;

/**
 * Một dòng của My Applications (5.1.38). Không có điểm AI hay nhận xét nội bộ (GBR-02).
 *
 * @param canWithdraw đơn còn ở vòng đầu (HR chưa duyệt) nên hiện nút "Rút đơn"
 */
public record MyApplicationResponse(
        Integer applicationId,
        Integer jobPostingId,
        String postingTitle,
        LocalDateTime submissionDate,
        String statusLabel,
        String statusBadge,
        boolean canWithdraw) {
}
