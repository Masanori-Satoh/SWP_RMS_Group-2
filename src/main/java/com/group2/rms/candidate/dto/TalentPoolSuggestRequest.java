package com.group2.rms.candidate.dto;

import jakarta.validation.constraints.*;
import java.util.List;

// choose talent appli of HR
public record TalentPoolSuggestRequest(
                @NotEmpty(message = "Chọn ít nhất một CV của ứng viên.") @Size(max = 20, message = "Mỗi lần chọn tối đa 20 ứng viên.") List<@NotNull @Positive Integer> sourceApplicationIds) {
}
