package com.group2.rms.offer.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectorDecisionRequest {
    @Size(max = 2000, message = "Phản hồi của Director không được vượt quá 2000 ký tự.")
    private String comments;
}
