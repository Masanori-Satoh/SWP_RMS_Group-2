package com.rms.feature.offer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO chứa thông tin phản hồi của HR đối với yêu cầu thương lượng từ ứng viên.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrNegotiationResponseDTO {

    @NotBlank(message = "Nội dung phản hồi không được để trống")
    private String hrResponseNotes;

    @NotNull(message = "ID của HR xử lý không được để trống")
    @Positive(message = "HR ID phải là số dương")
    private Long hrUserId;

    /**
     * Quyết định của HR:
     * - "APPROVE": HR duyệt chấp thuận thương lượng và chuyển về cho Hiring Manager điều chỉnh lương
     * - "REJECT": HR từ chối thương lượng lương, giữ nguyên mức đãi ngộ ban đầu
     */
    private String action;
}
