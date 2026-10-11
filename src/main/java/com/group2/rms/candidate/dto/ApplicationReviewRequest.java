package com.group2.rms.candidate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Một lượt duyệt hồ sơ từ form trên trang chi tiết. Nhận xét tùy chọn (SRS 5.6) và chỉ dùng nội bộ (GBR-02).
 *
 * @param decision {@code Pass}, {@code Hold} hoặc {@code Fail}
 */
public record ApplicationReviewRequest(
        @NotBlank(message = "Vui lòng chọn quyết định.")
        @Pattern(regexp = "Pass|Hold|Fail", message = "Quyết định không hợp lệ.")
        String decision,

        @Size(max = 1000, message = "Nhận xét tối đa 1000 ký tự.")
        String comments) {
}
