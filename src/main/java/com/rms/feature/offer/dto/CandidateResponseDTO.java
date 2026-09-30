package com.rms.feature.offer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO chứa phản hồi của ứng viên đối với Offer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateResponseDTO {

    @NotBlank(message = "Phản hồi của ứng viên không được để trống")
    @Pattern(regexp = "(?i)^(Accept|Decline|Negotiate)$", message = "Phản hồi phải là 'Accept', 'Decline', hoặc 'Negotiate'")
    private String response; // "Accept", "Decline", "Negotiate"

    @Positive(message = "Mức lương mong muốn đề xuất phải lớn hơn 0")
    private BigDecimal counterSalary; // Optional (dùng khi response = "Negotiate")

    private String candidateNotes; // Optional
}
