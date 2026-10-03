package com.group2.rms.offer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response chi tiết gói Offer (Screen 32):
 * Tổng hợp thông tin ứng viên (Group A), thông tin đãi ngộ (Group B),
 * lịch sử phê duyệt của Director (OfferApproval) và lịch sử đàm phán (OfferNegotiation).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferDetailResponse {

    // --- Group B: Thông tin Đãi ngộ & Điều khoản Offer ---
    private Integer offerId;
    private String offerStatus;
    private String offeredPositionTitle;
    private BigDecimal proposedSalary;
    private BigDecimal probationSalary;
    private Integer probationDays;
    private LocalDate expectedStartDate;
    private String workLocation;
    private String benefitsPackage;
    private Integer proposedById;
    private String proposedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- Group A: Thông tin Ứng viên & Vị trí tuyển dụng ---
    private Integer applicationId;
    private Integer candidateId;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String candidateAddress;
    private String appliedPosition;
    private String departmentName;
    private Integer requisitionId;
    private Integer jobPostingId;
    private String appliedCvUrl;

    // --- Thông tin Kết quả Phỏng vấn ---
    private String finalDecision;
    private String interviewSummaryComments;
    private BigDecimal recommendedSalary;
    private String hiringManagerName;
    private LocalDateTime interviewApprovedAt;

    // --- Audit Log: Lịch sử phê duyệt của Director (OfferApproval) ---
    private List<DirectorApprovalLog> approvalHistory;

    // --- Lịch sử đàm phán (OfferNegotiation) ---
    private List<NegotiationRound> negotiationHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectorApprovalLog {
        private Integer approvalId;
        private Integer directorId;
        private String directorName;
        private String status;
        private String directorComments;
        private LocalDateTime approvedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NegotiationRound {
        private Integer negotiationId;
        private BigDecimal candidateCounterSalary;
        private String candidateNotes;
        private String hrResponseNotes;
        private LocalDateTime negotiationDate;
    }
}
