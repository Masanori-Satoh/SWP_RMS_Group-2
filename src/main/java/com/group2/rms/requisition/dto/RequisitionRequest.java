package com.group2.rms.requisition.dto;

import com.group2.rms.requisition.validator.RequisitionValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dữ liệu form Tạo / Sửa yêu cầu tuyển dụng (requisitions/form.html).
 *
 * Annotation ở đây là tầng 2 (Bean Validation, AG §2.4): chặn sớm giá trị sai định dạng / quá giới hạn,
 * lỗi tự vào BindingResult khi controller nhận bằng @Valid. Giới hạn và câu lỗi lấy từ RequisitionValidator
 * nên chỉ sửa một chỗ.
 * Không đặt @NotBlank: "Lưu bản nháp" được để trống, phần bắt buộc do RequisitionValidator kiểm tra
 * (phân biệt nháp / gửi duyệt). RequisitionValidator vẫn kiểm tra lại toàn bộ vì service không tin controller.
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
    private Integer recruitmentRound;
    /** Phiếu nguồn khi sao chép sang đợt mới; server tự quy về phiếu đợt 1 và tính lại số đợt. */
    private Integer originalRequisitionId;
    /** Tên vị trí của phiếu đợt 1, chỉ để form đổi số đợt hiển thị khi người dùng sửa tên; server không dùng giá trị này. */
    private String originalTitle;
    /** New / Replacement, không bắt buộc. */
    private String hiringType;

    @Min(value = RequisitionValidator.POSITIONS_MIN, message = RequisitionValidator.POSITIONS_RANGE_MESSAGE)
    @Max(value = RequisitionValidator.POSITIONS_MAX, message = RequisitionValidator.POSITIONS_RANGE_MESSAGE)
    private Integer numberOfPositions;
    private String employmentType;

    // NOTE: @Digits integer = 16 theo cột DECIMAL(18,2), chỉ để kiểm phần thập phân;
    // vượt mức tối đa do @DecimalMax báo (tránh hai câu lỗi cho cùng một ô)
    @DecimalMin(value = "0", inclusive = false, message = RequisitionValidator.SALARY_FORMAT_MESSAGE)
    @Digits(integer = 16, fraction = 2, message = RequisitionValidator.SALARY_FORMAT_MESSAGE)
    @DecimalMax(value = RequisitionValidator.SALARY_MAX_TEXT, message = RequisitionValidator.SALARY_MAX_MESSAGE)
    private BigDecimal minSalary;

    @DecimalMin(value = "0", inclusive = false, message = RequisitionValidator.SALARY_FORMAT_MESSAGE)
    @Digits(integer = 16, fraction = 2, message = RequisitionValidator.SALARY_FORMAT_MESSAGE)
    @DecimalMax(value = RequisitionValidator.SALARY_MAX_TEXT, message = RequisitionValidator.SALARY_MAX_MESSAGE)
    private BigDecimal maxSalary;
    private String gender;

    @Size(max = RequisitionValidator.SHORT_FIELD_CHAR_LIMIT, message = RequisitionValidator.SHORT_FIELD_TOO_LONG)
    private String workLocation;
    private String workModel;

    @Min(value = RequisitionValidator.PROBATION_MIN_DAYS, message = RequisitionValidator.PROBATION_RANGE_MESSAGE)
    @Max(value = RequisitionValidator.PROBATION_MAX_DAYS, message = RequisitionValidator.PROBATION_RANGE_MESSAGE)
    private Integer probationDays;

    // NOTE: 3 ô nhiều dòng không đặt @Size: trình duyệt gửi xuống dòng là \r\n (2 ký tự), Bean Validation
    // chạy trước khi RequisitionValidator.cleanMultiline đổi về \n, nên đếm ở đây sẽ chặn oan đoạn văn gần giới hạn.
    private String reasonForHiring;
    private String jobDescription;
    private String requirementDetails;

    @Valid
    @Builder.Default
    private List<ScreeningCriteriaRequest> screeningCriteria = new ArrayList<>();
}
