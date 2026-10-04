package com.group2.rms.offer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response cho danh sách ứng viên đỗ phỏng vấn (FinalDecision = Passed)
 * để HR chọn và tạo Offer Proposal (Group A: Read Only).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassedCandidateResponse {

    private Integer applicationId;
    private Integer candidateId;
    private String candidateName;
    private String email;
    private String phoneNumber;
    private String appliedPosition;
    private String departmentName;
    private Integer requisitionId;
    private Integer jobPostingId;
    private String workLocation;

    // Thông tin tóm tắt kết quả phỏng vấn
    private Integer finalResultId;
    private String finalDecision;
    private String interviewSummaryComments;
    private BigDecimal recommendedSalary;
    private LocalDateTime interviewApprovedAt;
    private String hiringManagerName;

    // Thông tin Offer hiện tại (nếu có, thuộc Nhóm A cho phép tạo đè theo quy tắc GBR-07)
    private String existingOfferStatus;
    private Integer existingOfferId;
}
