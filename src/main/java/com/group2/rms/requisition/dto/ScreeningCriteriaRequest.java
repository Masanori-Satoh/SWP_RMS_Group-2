package com.group2.rms.requisition.dto;

import com.group2.rms.requisition.validator.RequisitionValidator;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Một dòng tiêu chí sàng lọc trên form yêu cầu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningCriteriaRequest {
    // Annotation chỉ chặn giá trị quá giới hạn; dòng trống / bắt buộc / trùng tên / tổng 100% do RequisitionValidator kiểm tra
    private Integer criteriaId;

    @Size(max = RequisitionValidator.CRITERIA_NAME_CHAR_LIMIT, message = RequisitionValidator.CRITERIA_NAME_TOO_LONG)
    private String criteriaName;
    private String criteriaType;

    @Size(max = RequisitionValidator.SHORT_FIELD_CHAR_LIMIT, message = RequisitionValidator.SHORT_FIELD_TOO_LONG)
    private String requiredValue;

    // NOTE: @Digits integer = 3 theo cột DECIMAL(5,2)
    @DecimalMin(value = "0", inclusive = false, message = RequisitionValidator.WEIGHT_MESSAGE)
    @DecimalMax(value = RequisitionValidator.WEIGHT_MAX_TEXT, message = RequisitionValidator.WEIGHT_MESSAGE)
    @Digits(integer = 3, fraction = 2, message = RequisitionValidator.WEIGHT_MESSAGE)
    private BigDecimal weight;
    private Boolean isMandatory;
}
