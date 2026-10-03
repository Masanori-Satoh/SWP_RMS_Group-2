package com.group2.rms.offer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response trả về thông tin tóm tắt Offer Proposal (phục vụ danh sách Offer).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferResponse {

    private Integer offerId;
    private Integer applicationId;
    private String candidateName;
    private String candidateEmail;
    private String offeredPositionTitle;
    private BigDecimal proposedSalary;
    private BigDecimal probationSalary;
    private LocalDate expectedStartDate;
    private String workLocation;
    private String benefitsPackage;
    private Integer proposedById;
    private String proposedByName;
    private String offerStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
