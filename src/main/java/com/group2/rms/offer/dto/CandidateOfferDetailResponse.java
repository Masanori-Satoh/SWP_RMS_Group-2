package com.group2.rms.offer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Toàn bộ thông tin Thư Mời Nhận Việc (Offer Letter) hiển thị cho ứng viên trên màn hình riêng.
 * Bảo mật: Ứng viên chỉ thấy các thông tin chính thức của offer và người phụ trách HR,
 * không lộ ghi chú nội bộ hay lịch sử phê duyệt của Giám đốc.
 */
public record CandidateOfferDetailResponse(
        Integer offerId,
        String candidateName,
        String candidateEmail,
        String candidatePhone,
        String candidateAddress,
        String postingTitle,
        String offeredPositionTitle,
        String departmentName,
        BigDecimal proposedSalary,
        BigDecimal probationSalary,
        Integer probationRatePercent,
        String probationDuration,
        LocalDate expectedStartDate,
        String workLocation,
        String benefitsPackage,
        String offerStatus,
        String statusLabel,
        String statusBadge,
        boolean canRespond,
        String hrContactName,
        String hrContactEmail,
        String declineReason,
        LocalDateTime sentAt,
        LocalDateTime updatedAt) {

    public CandidateOfferDetailResponse(
            Integer offerId, String candidateName, String candidateEmail, String candidatePhone, String candidateAddress,
            String postingTitle, String offeredPositionTitle, String departmentName, BigDecimal proposedSalary,
            BigDecimal probationSalary, Integer probationRatePercent, String probationDuration, LocalDate expectedStartDate,
            String workLocation, String benefitsPackage, String offerStatus, String statusLabel, String statusBadge,
            boolean canRespond, String hrContactName, String hrContactEmail, LocalDateTime sentAt, LocalDateTime updatedAt) {
        this(offerId, candidateName, candidateEmail, candidatePhone, candidateAddress, postingTitle, offeredPositionTitle,
                departmentName, proposedSalary, probationSalary, probationRatePercent, probationDuration, expectedStartDate,
                workLocation, benefitsPackage, offerStatus, statusLabel, statusBadge, canRespond, hrContactName,
                hrContactEmail, null, sentAt, updatedAt);
    }
}
