package com.group2.rms.requisition.dto;

import com.group2.rms.requisition.validator.RequisitionValidator;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/*
 * DTO: form tạo / sửa yêu cầu; annotation chặn sớm định dạng, phần bắt buộc do RequisitionValidator kiểm tra.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequisitionRequest {
    private String action;

    @Size(max = RequisitionValidator.TITLE_CHAR_LIMIT, message = RequisitionValidator.TITLE_TOO_LONG)
    private String title;
    private Integer departmentId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expectedStaffingDate;
    /**
     * Phiếu gốc khi sao chép, chỉ dùng để truy vết nguồn.
     */
    private Integer originalRequisitionId;
    /**
     * Tên vị trí của phiếu gốc để hiển thị nguồn sao chép.
     */
    private String originalTitle;

    @Min(value = RequisitionValidator.POSITIONS_MIN, message = RequisitionValidator.POSITIONS_MIN_MESSAGE)
    private Integer numberOfPositions;
    private String employmentType;

    @DecimalMin(value = "0", inclusive = false, message = RequisitionValidator.SALARY_FORMAT_MESSAGE)
    @Digits(integer = 16, fraction = 2, message = RequisitionValidator.SALARY_FORMAT_MESSAGE)
    @DecimalMax(value = RequisitionValidator.SALARY_MAX_TEXT, message = RequisitionValidator.SALARY_MAX_MESSAGE)
    private BigDecimal minSalary;

    @DecimalMin(value = "0", inclusive = false, message = RequisitionValidator.SALARY_FORMAT_MESSAGE)
    @Digits(integer = 16, fraction = 2, message = RequisitionValidator.SALARY_FORMAT_MESSAGE)
    @DecimalMax(value = RequisitionValidator.SALARY_MAX_TEXT, message = RequisitionValidator.SALARY_MAX_MESSAGE)
    private BigDecimal maxSalary;
    @Size(max = RequisitionValidator.SHORT_FIELD_CHAR_LIMIT, message = RequisitionValidator.SHORT_FIELD_TOO_LONG)
    private String workLocation;
    private String workModel;

    private String reasonForHiring;
    private String jobDescription;
    private String requirementDetails;
}
