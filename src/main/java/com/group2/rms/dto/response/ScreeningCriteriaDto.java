package com.group2.rms.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningCriteriaDto {
    private Integer criteriaId;
    private String criteriaName;
    private String criteriaType;
    private String requiredValue;
    private BigDecimal weight;
    private Boolean isMandatory;
}
