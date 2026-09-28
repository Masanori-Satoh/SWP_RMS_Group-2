package com.group2.rms.dto.request;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningCriteriaRequestDto {
    private String criteriaName;
    private String criteriaType;
    private String requiredValue;
    private BigDecimal weight;
    private Boolean isMandatory;
}
