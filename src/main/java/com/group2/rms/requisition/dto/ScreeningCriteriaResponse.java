package com.group2.rms.requisition.dto;

import java.math.BigDecimal;

import lombok.Builder;

/** Một tiêu chí sàng lọc ở màn chi tiết yêu cầu. */
@Builder
public record ScreeningCriteriaResponse(
        Integer criteriaId,
        String criteriaName,
        String criteriaType,
        String requiredValue,
        BigDecimal weight,
        Boolean isMandatory,
        String criteriaTypeLabel) {   // "Kỹ năng", "Học vấn"... (RequisitionLabels.criteriaType)
}
