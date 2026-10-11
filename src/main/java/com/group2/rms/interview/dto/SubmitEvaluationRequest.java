package com.group2.rms.interview.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Phiếu đánh giá do thành viên hội đồng nộp (Submit Interview Evaluate - SRS 5.1.29).
 * InterviewId lấy từ đường dẫn, không nhận từ body.
 *
 * Từng điểm được bỏ trống (DB cho NULL): mỗi thành viên chỉ chấm mục thuộc chuyên môn của mình,
 * ví dụ HR chỉ chấm Kỹ năng mềm / Phù hợp văn hóa. Nhưng phải chấm ít nhất một mục.
 */
public record SubmitEvaluationRequest(
        @Min(value = 1, message = "Điểm kỹ thuật phải từ 1 đến 5.")
        @Max(value = 5, message = "Điểm kỹ thuật phải từ 1 đến 5.")
        Integer technicalScore,

        @Min(value = 1, message = "Điểm kỹ năng mềm phải từ 1 đến 5.")
        @Max(value = 5, message = "Điểm kỹ năng mềm phải từ 1 đến 5.")
        Integer softSkillScore,

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

    /** Lỗi gắn với ô "anyScoreGiven" (giao diện hiện dưới nhóm điểm). */
    @AssertTrue(message = "Vui lòng chấm ít nhất một mục điểm.")
    public boolean isAnyScoreGiven() {
        return technicalScore != null || softSkillScore != null || culturalFitScore != null;
    }
}
