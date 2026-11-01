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
        String offerCode,
        String candidateName,
        String candidateEmail,
        String candidatePhone,
        String candidateAddress,
        String postingTitle,
        String offeredPositionTitle,
        String departmentName,
        String reportTo,
        String employmentType,
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
        Integer responsePeriodDays,
        LocalDateTime responseDeadline,
        LocalDateTime sentAt,
        LocalDateTime updatedAt,
        Integer applicationId,
        String applicationCode,
        LocalDateTime submissionDate) {

    public CandidateOfferDetailResponse(
            Integer offerId, String offerCode, String candidateName, String candidateEmail, String candidatePhone,
            String candidateAddress, String postingTitle, String offeredPositionTitle, String departmentName,
            String reportTo, String employmentType, BigDecimal proposedSalary, BigDecimal probationSalary,
            Integer probationRatePercent, String probationDuration, LocalDate expectedStartDate,
            String workLocation, String benefitsPackage, String offerStatus, String statusLabel, String statusBadge,
            boolean canRespond, String hrContactName, String hrContactEmail, String declineReason,
            Integer responsePeriodDays, LocalDateTime responseDeadline, LocalDateTime sentAt, LocalDateTime updatedAt) {
        this(offerId, offerCode, candidateName, candidateEmail, candidatePhone, candidateAddress,
                postingTitle, offeredPositionTitle, departmentName, reportTo, employmentType,
                proposedSalary, probationSalary, probationRatePercent, probationDuration,
                expectedStartDate, workLocation, benefitsPackage, offerStatus, statusLabel,
                statusBadge, canRespond, hrContactName, hrContactEmail, declineReason,
                responsePeriodDays, responseDeadline, sentAt, updatedAt,
                offerId != null ? offerId : 1,
                offerId != null ? String.format("#CAN-%04d", offerId) : "#CAN-0001",
                sentAt != null ? sentAt.minusDays(11) : LocalDateTime.now().minusDays(11));
    }

    public CandidateOfferDetailResponse(
            Integer offerId, String candidateName, String candidateEmail, String candidatePhone, String candidateAddress,
            String postingTitle, String offeredPositionTitle, String departmentName, BigDecimal proposedSalary,
            BigDecimal probationSalary, Integer probationRatePercent, String probationDuration, LocalDate expectedStartDate,
            String workLocation, String benefitsPackage, String offerStatus, String statusLabel, String statusBadge,
            boolean canRespond, String hrContactName, String hrContactEmail, LocalDateTime sentAt, LocalDateTime updatedAt) {
        this(offerId, "OFFR-2026-" + String.format("%04d", offerId != null ? offerId : 0),
                candidateName, candidateEmail, candidatePhone, candidateAddress, postingTitle, offeredPositionTitle,
                departmentName, (departmentName != null ? "Trưởng phòng " + departmentName : "Ban Giám đốc"),
                "Toàn thời gian (08:30 – 17:30, Thứ Hai đến Thứ Sáu)", proposedSalary, probationSalary,
                probationRatePercent, probationDuration, expectedStartDate, workLocation, benefitsPackage,
                offerStatus, statusLabel, statusBadge, canRespond, hrContactName, hrContactEmail, null,
                null, null, sentAt, updatedAt);
    }

    public CandidateOfferDetailResponse(
            Integer offerId, String candidateName, String candidateEmail, String candidatePhone, String candidateAddress,
            String postingTitle, String offeredPositionTitle, String departmentName, BigDecimal proposedSalary,
            BigDecimal probationSalary, Integer probationRatePercent, String probationDuration, LocalDate expectedStartDate,
            String workLocation, String benefitsPackage, String offerStatus, String statusLabel, String statusBadge,
            boolean canRespond, String hrContactName, String hrContactEmail, String declineReason, LocalDateTime sentAt,
            LocalDateTime updatedAt) {
        this(offerId, "OFFR-2026-" + String.format("%04d", offerId != null ? offerId : 0),
                candidateName, candidateEmail, candidatePhone, candidateAddress, postingTitle, offeredPositionTitle,
                departmentName, (departmentName != null ? "Trưởng phòng " + departmentName : "Ban Giám đốc"),
                "Toàn thời gian (08:30 – 17:30, Thứ Hai đến Thứ Sáu)", proposedSalary, probationSalary,
                probationRatePercent, probationDuration, expectedStartDate, workLocation, benefitsPackage,
                offerStatus, statusLabel, statusBadge, canRespond, hrContactName, hrContactEmail, declineReason,
                null, null, sentAt, updatedAt);
    }

    public CandidateOfferDetailResponse(
            Integer offerId, String candidateName, String candidateEmail, String candidatePhone, String candidateAddress,
            String postingTitle, String offeredPositionTitle, String departmentName, BigDecimal proposedSalary,
            BigDecimal probationSalary, Integer probationRatePercent, String probationDuration, LocalDate expectedStartDate,
            String workLocation, String benefitsPackage, String offerStatus, String statusLabel, String statusBadge,
            boolean canRespond, String hrContactName, String hrContactEmail, String declineReason,
            Integer responsePeriodDays, LocalDateTime responseDeadline, LocalDateTime sentAt, LocalDateTime updatedAt) {
        this(offerId, "OFFR-2026-" + String.format("%04d", offerId != null ? offerId : 0),
                candidateName, candidateEmail, candidatePhone, candidateAddress, postingTitle, offeredPositionTitle,
                departmentName, (departmentName != null ? "Trưởng phòng " + departmentName : "Ban Giám đốc"),
                "Toàn thời gian (08:30 – 17:30, Thứ Hai đến Thứ Sáu)", proposedSalary, probationSalary,
                probationRatePercent, probationDuration, expectedStartDate, workLocation, benefitsPackage,
                offerStatus, statusLabel, statusBadge, canRespond, hrContactName, hrContactEmail, declineReason,
                responsePeriodDays, responseDeadline, sentAt, updatedAt);
    }

    public boolean isExpired() {
        return "Đã quá hạn phản hồi".equals(statusLabel)
                || (!canRespond && "Sent_Candidate".equals(offerStatus));
    }
}
