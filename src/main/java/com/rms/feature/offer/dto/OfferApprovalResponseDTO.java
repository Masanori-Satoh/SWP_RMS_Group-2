package com.rms.feature.offer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO hiển thị thông tin lịch sử phê duyệt của Director.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferApprovalResponseDTO {

    private Long offerApprovalId;
    private Long directorId;
    private String directorName;
    private String status;
    private String directorComments;
    private LocalDateTime approvedAt;
}
