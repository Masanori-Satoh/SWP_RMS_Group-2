package com.rms.feature.offer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO hiển thị thông tin lịch sử thương lượng lương của ứng viên.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferNegotiationResponseDTO {

    private Long negotiationId;
    private BigDecimal candidateCounterSalary;
    private String candidateNotes;
    private String hrResponseNotes;
    private LocalDateTime negotiationDate;
}
