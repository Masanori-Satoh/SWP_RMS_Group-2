package com.rms.feature.offer.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO đại diện cho yêu cầu tạo Offer Proposal mới.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOfferRequestDTO {

    @NotNull(message = "Application ID không được để trống")
    @Positive(message = "Application ID phải là số dương")
    private Long applicationId;

    @NotNull(message = "Mức lương chính thức đề xuất không được để trống")
    @Positive(message = "Lương đề xuất phải lớn hơn 0")
    @DecimalMin(value = "0.0", inclusive = false, message = "Lương đề xuất phải lớn hơn 0")
    private BigDecimal proposedSalary;

    @NotNull(message = "Mức lương thử việc không được để trống")
    @Positive(message = "Lương thử việc phải lớn hơn 0")
    @DecimalMin(value = "0.0", inclusive = false, message = "Lương thử việc phải lớn hơn 0")
    private BigDecimal probationSalary;

    @Positive(message = "Số ngày thử việc phải là số dương")
    private Integer probationDays;

    @NotBlank(message = "Vị trí công việc không được để trống")
    private String proposedPosition;

    private String workLocation;

    @NotNull(message = "ID người đề xuất không được để trống")
    @Positive(message = "ID người đề xuất phải là số dương")
    private Long proposedById;

    /**
     * true: lưu bản thảo (Draft), false/null: gửi thẳng Pending_Director_Approval
     */
    private Boolean isDraft;

    @AssertTrue(message = "Lương thử việc không được cao hơn lương chính thức (phải thấp hơn hoặc bằng lương chính thức)")
    public boolean isProbationSalaryNotExceedProposed() {
        if (probationSalary == null || proposedSalary == null) return true;
        return probationSalary.compareTo(proposedSalary) <= 0;
    }

    @AssertTrue(message = "Lương thử việc phải đạt tối thiểu 85% lương chính thức theo Luật Lao động (BR-OFF-01)")
    public boolean isProbationSalaryAtLeast85Percent() {
        if (probationSalary == null || proposedSalary == null) return true;
        return probationSalary.compareTo(proposedSalary.multiply(new BigDecimal("0.85"))) >= 0;
    }
}
