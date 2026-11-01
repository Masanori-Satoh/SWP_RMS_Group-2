package com.group2.rms.offer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Một thư mời nhận việc hiển thị cho chính ứng viên. Chỉ có thông tin chính thức của offer,
 * không có người đề xuất, lịch sử duyệt của Director hay ghi chú nội bộ.
 */
public record CandidateOfferResponse(
        Integer offerId,
        String postingTitle,
        String offeredPositionTitle,
        BigDecimal proposedSalary,
        BigDecimal probationSalary,
        LocalDate expectedStartDate,
        String workLocation,
        String benefitsPackage,
        String statusLabel,
        String statusBadge,
        boolean canRespond,
        Integer responsePeriodDays,
        LocalDateTime responseDeadline) {

    public CandidateOfferResponse(
            Integer offerId, String postingTitle, String offeredPositionTitle,
            BigDecimal proposedSalary, BigDecimal probationSalary, LocalDate expectedStartDate,
            String workLocation, String benefitsPackage, String statusLabel,
            String statusBadge, boolean canRespond) {
        this(offerId, postingTitle, offeredPositionTitle, proposedSalary, probationSalary,
                expectedStartDate, workLocation, benefitsPackage, statusLabel, statusBadge,
                canRespond, null, null);
    }

    public boolean isExpired() {
        return "Đã quá hạn phản hồi".equals(statusLabel);
    }
}
