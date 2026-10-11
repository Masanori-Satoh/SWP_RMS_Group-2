package com.group2.rms.interview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Phiếu đánh giá do thành viên hội đồng nộp (Submit Interview Evaluate - SRS 5.1.29).
 * InterviewId lấy từ đường dẫn, không nhận từ body.
 */
public record SubmitEvaluationRequest(
        @NotNull(message = "Vui lòng chấm điểm kỹ thuật.")
        @Min(value = 1, message = "Điểm kỹ thuật phải từ 1 đến 5.")
        @Max(value = 5, message = "Điểm kỹ thuật phải từ 1 đến 5.")
        Integer technicalScore,

        @NotNull(message = "Vui lòng chấm điểm kỹ năng mềm.")
        @Min(value = 1, message = "Điểm kỹ năng mềm phải từ 1 đến 5.")
        @Max(value = 5, message = "Điểm kỹ năng mềm phải từ 1 đến 5.")
        Integer softSkillScore,

        @NotNull(message = "Vui lòng chấm điểm phù hợp văn hóa.")
        @Min(value = 1, message = "Điểm phù hợp văn hóa phải từ 1 đến 5.")
        @Max(value = 5, message = "Điểm phù hợp văn hóa phải từ 1 đến 5.")
        Integer culturalFitScore,

        @NotBlank(message = "Vui lòng chọn đề xuất.")
        @Pattern(regexp = "Hire|No_Hire|Consider", message = "Đề xuất không hợp lệ.")
        String recommendation,

        @Size(max = 2000, message = "Điểm mạnh tối đa 2000 ký tự.")
        String strengths,

        @Size(max = 2000, message = "Điểm yếu tối đa 2000 ký tự.")
        String weaknesses,

        @Size(max = 2000, message = "Nhận xét tối đa 2000 ký tự.")
        String comments
) {
}
