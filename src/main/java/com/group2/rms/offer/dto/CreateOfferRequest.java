package com.group2.rms.offer.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request tạo mới Offer Proposal.
 * Tích hợp quy tắc BR-OFF-01: Lương thử việc >= 85% lương chính thức.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOfferRequest {

    @NotNull(message = "Vui lòng chọn ứng viên đỗ phỏng vấn.")
    private Integer applicationId;

    @NotBlank(message = "Vị trí chức danh đề xuất không được để trống.")
    @Size(max = 150, message = "Vị trí chức danh đề xuất không được vượt quá 150 ký tự.")
    private String offeredPositionTitle;

    @NotNull(message = "Mức lương chính thức không được để trống.")
    @Positive(message = "Mức lương chính thức phải lớn hơn 0.")
    private BigDecimal proposedSalary;

    @NotNull(message = "Mức lương thử việc không được để trống.")
    @Positive(message = "Mức lương thử việc phải lớn hơn 0.")
    private BigDecimal probationSalary;

    @NotNull(message = "Ngày bắt đầu dự kiến không được để trống.")
    @Future(message = "Ngày bắt đầu dự kiến phải lớn hơn ngày hiện tại.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expectedStartDate;

    @NotBlank(message = "Địa điểm làm việc không được để trống.")
    @Size(max = 255, message = "Địa điểm làm việc không được vượt quá 255 ký tự.")
    private String workLocation;

    @Size(max = 2000, message = "Gói phúc lợi không được vượt quá 2000 ký tự.")
    private String benefitsPackage;

    private Integer proposedById;

    @NotNull(message = "Thời gian thử việc không được để trống.")
    @Positive(message = "Thời gian thử việc phải lớn hơn 0.")
    @Max(value = 365, message = "Thời gian thử việc không được vượt quá 365 ngày.")
    private Integer probationDays;

    /**
     * true: Lưu bản thảo (Draft)
     * false hoặc null: Trình duyệt Director (Pending_Director)
     */
    private Boolean isDraft;

    public Boolean getIsDraft() {
        return this.isDraft;
    }

    public void setIsDraft(Boolean isDraft) {
        this.isDraft = isDraft;
    }

    // Alias hỗ trợ nếu caller dùng tên proposedPosition
    public String getProposedPosition() {
        return this.offeredPositionTitle;
    }

    public void setProposedPosition(String proposedPosition) {
        this.offeredPositionTitle = proposedPosition;
    }

    /**
     * Quy tắc BR-OFF-01: Kiểm tra lương thử việc >= 85% lương chính thức ở tầng
     * Bean Validation
     */
    @AssertTrue(message = "Lương thử việc phải đạt tối thiểu 85% lương chính thức.")
    public boolean isProbationSalaryValid() {
        if (proposedSalary == null || probationSalary == null) {
            return true; // Để @NotNull xử lý
        }
        if (proposedSalary.compareTo(BigDecimal.ZERO) <= 0 || probationSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return true; // Để @Positive xử lý
        }
        BigDecimal minProbation = proposedSalary.multiply(new BigDecimal("0.85"));
        return probationSalary.compareTo(minProbation) >= 0;
    }

    /**
     * Ràng buộc: Lương thử việc không được vượt quá lương chính thức
     */
    @AssertTrue(message = "Lương thử việc không được vượt quá lương chính thức.")
    public boolean isProbationSalaryNotExceedProposed() {
        if (proposedSalary == null || probationSalary == null) {
            return true; // Để @NotNull xử lý
        }
        return probationSalary.compareTo(proposedSalary) <= 0;
    }

    /**
     * Ràng buộc: Ngày bắt đầu dự kiến không được quá 2 năm tới
     */
    @AssertTrue(message = "Năm bắt đầu dự kiến không hợp lệ (không quá 2 năm tới).")
    public boolean isExpectedStartDateValid() {
        if (expectedStartDate == null) {
            return true; // Để @NotNull xử lý
        }
        return expectedStartDate.getYear() <= (LocalDate.now().getYear() + 2);
    }
}
