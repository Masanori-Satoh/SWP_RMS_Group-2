package com.rms.feature.offer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO chứa thông tin phê duyệt từ Director.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectorApprovalDTO {

    @NotNull(message = "ID của Director không được để trống")
    @Positive(message = "Director ID phải là số dương")
    private Long directorId;

    @NotBlank(message = "Trạng thái phê duyệt không được để trống")
    @Pattern(regexp = "(?i)^(Approved|Rejected)$", message = "Trạng thái phê duyệt phải là 'Approved' hoặc 'Rejected'")
    private String status; // "Approved" hoặc "Rejected"

    private String directorComments;
}
