package com.group2.rms.interview.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Hiring Manager chốt kết quả cuối của một buổi phỏng vấn (bảng InterviewFinalResult).
 * InterviewId lấy từ đường dẫn, không nhận từ body.
 */
public record SubmitFinalResultRequest(
        @NotBlank(message = "Vui lòng chọn kết quả.")
        @Pattern(regexp = "Passed|Failed|Considering", message = "Kết quả không hợp lệ.")
        String finalDecision,

        @DecimalMin(value = "0", message = "Mức lương đề xuất không được âm.")
        @Digits(integer = 16, fraction = 2, message = "Mức lương đề xuất không hợp lệ.")
        BigDecimal recommendedSalary,

        @Size(max = 4000, message = "Nhận xét tổng hợp tối đa 4000 ký tự.")
        String finalSummaryComments
) {
}
