package com.rms.feature.offer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO trả về chi tiết đầy đủ của OfferProposal (gồm thông tin offer, history approval, history negotiation).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferResponseDTO {

    private Long offerId;
    private Long applicationId;
    private Long candidateId;
    private String candidateName;
    private Long jobPostingId;
    private String jobTitle;
    private BigDecimal proposedSalary;
    private BigDecimal probationSalary;
    private Integer probationDays;
    private String proposedPosition;
    private String workLocation;
    private Long proposedById;
    private String proposedByName;
    private String offerStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<OfferApprovalResponseDTO> approvalHistory;
    private List<OfferNegotiationResponseDTO> negotiationHistory;
}
