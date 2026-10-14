package com.group2.rms.candidate.dto;

import jakarta.validation.constraints.*;
// note for candidate
public record TalentPoolNoteRequest(
        @NotBlank(message = "Vui lòng nhập ghi chú.") 
        @Size(max = 1000, message = "Ghi chú tối đa 1.000 ký tự.") 
        String content) {
}
