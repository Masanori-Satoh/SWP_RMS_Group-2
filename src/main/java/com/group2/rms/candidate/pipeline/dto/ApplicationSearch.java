package com.group2.rms.candidate.pipeline.dto;

import com.group2.rms.candidate.domain.rule.ApplicationStage;
import com.group2.rms.candidate.domain.rule.ApplicationStatusLabels;

import java.util.Arrays;

/**
 * Bộ lọc danh sách hồ sơ (query string của {@code GET /applications}).
 * Giá trị lạ không báo lỗi mà bị bỏ qua: đây là bộ lọc, không phải form nhập liệu.
 *
 * @param status trạng thái lưu DB; giữ để link cũ không gãy (trang lọc bằng {@code stage})
 * @param sort   {@code score} (mặc định: điểm AI cao nhất) hoặc {@code newest}
 * @param stage  tên một {@link ApplicationStage} (ví dụ {@code INTERVIEW})
 */
public record ApplicationSearch(Integer jobPostingId, String status, String keyword, String sort, String stage) {

    public static final String SORT_SCORE = "score";
    public static final String SORT_NEWEST = "newest";
    static final int KEYWORD_MAX_LENGTH = 100;

    public ApplicationSearch normalized() {
        String cleanStatus = status != null && ApplicationStatusLabels.STATUSES.contains(status) ? status : null;
        String cleanKeyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
        if (cleanKeyword != null && cleanKeyword.length() > KEYWORD_MAX_LENGTH) {
            cleanKeyword = cleanKeyword.substring(0, KEYWORD_MAX_LENGTH);
        }
        String cleanSort = SORT_NEWEST.equals(sort) ? SORT_NEWEST : SORT_SCORE;
        String cleanStage = Arrays.stream(ApplicationStage.values()).map(Enum::name)
                .filter(name -> name.equals(stage)).findFirst().orElse(null);
        return new ApplicationSearch(jobPostingId, cleanStatus, cleanKeyword, cleanSort, cleanStage);
    }

    /** Giai đoạn đang lọc; {@code null} nếu không lọc (gọi trên bản đã {@link #normalized()}). */
    public ApplicationStage stageFilter() {
        return stage == null ? null : ApplicationStage.valueOf(stage);
    }
}
