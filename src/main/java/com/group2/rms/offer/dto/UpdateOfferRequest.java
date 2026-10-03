package com.group2.rms.offer.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request chỉnh sửa Offer Proposal (Chỉ áp dụng khi Offer ở trạng thái Draft hoặc Rejected).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOfferRequest {

    private String offeredPositionTitle;

    @NotNull(message = "Lương chính thức không được để trống.")
    @Positive(message = "Lương chính thức phải lớn hơn 0.")
    private BigDecimal proposedSalary;

    @NotNull(message = "Lương thử việc không được để trống.")
    @Positive(message = "Lương thử việc phải lớn hơn 0.")
    private BigDecimal probationSalary;

    private Integer probationDays;

    private LocalDate expectedStartDate;

    private String workLocation;

    private String benefitsPackage;

    /**
     * true: Tiếp tục lưu nháp (chỉ khi trạng thái hiện tại là Draft)
     * false hoặc null: Trình duyệt lại Director (chuyển sang Pending_Director)
     */
    private Boolean isDraft;

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
            return true;
        }
        if (proposedSalary.compareTo(BigDecimal.ZERO) <= 0 || probationSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        BigDecimal minProbation = proposedSalary.multiply(new BigDecimal("0.85"));
        return probationSalary.compareTo(minProbation) >= 0;
    }
}
