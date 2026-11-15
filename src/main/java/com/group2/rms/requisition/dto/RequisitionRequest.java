package com.group2.rms.requisition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/*
 * DTO: form tạo / sửa yêu cầu; RequisitionValidator kiểm tra nội dung khi gửi duyệt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequisitionRequest {
    private String action;

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

    private Integer numberOfPositions;
    private String employmentType;
    private BigDecimal minSalary;

    private BigDecimal maxSalary;
    private String workLocation;
    private String workModel;

    private String reasonForHiring;
    private String jobDescription;
    private String requirementDetails;
}
