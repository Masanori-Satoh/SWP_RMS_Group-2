package com.group2.rms.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO tạo mới Offer Proposal.
 * Tích hợp quy tắc BR-OFF-01: Lương thử việc >= 85% lương chính thức.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOfferRequestDto {

    @NotNull(message = "Application ID không được để trống.")
    private Integer applicationId;

    @NotBlank(message = "Vị trí đề xuất không được để trống.")
    private String offeredPositionTitle;

    @NotNull(message = "Lương chính thức không được để trống.")
    @Positive(message = "Lương chính thức phải lớn hơn 0.")
    private BigDecimal proposedSalary;

    @NotNull(message = "Lương thử việc không được để trống.")
    @Positive(message = "Lương thử việc phải lớn hơn 0.")
    private BigDecimal probationSalary;

    private LocalDate expectedStartDate;

    private String workLocation;

    private String benefitsPackage;

    private Integer proposedById;

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
     * Quy tắc BR-OFF-01: Kiểm tra lương thử việc >= 85% lương chính thức ở tầng Bean Validation
     */
    @AssertTrue(message = "Lương thử việc phải đạt tối thiểu 85% lương chính thức (quy định BR-OFF-01).")
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
}
