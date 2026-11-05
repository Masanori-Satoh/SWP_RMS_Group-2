package com.group2.rms.candidate.application.pipeline.dto;

import com.group2.rms.candidate.domain.rule.ApplicationStage;
import com.group2.rms.candidate.domain.rule.ApplicationStatusLabels;
import com.group2.rms.core.util.ListFilters;

import java.util.Arrays;

/**
 * Bộ lọc danh sách hồ sơ (query string của {@code GET /applications}).
 * Giá trị lạ không báo lỗi mà bị bỏ qua: đây là bộ lọc, không phải form nhập liệu.
 *
 * @param status trạng thái lưu DB; giữ để link cũ không gãy (trang lọc bằng {@code stage})
 * @param stage  tên một {@link ApplicationStage} (ví dụ {@code INTERVIEW})
 */
public record ApplicationSearch(Integer jobPostingId, String status, String keyword, String stage) {

    public ApplicationSearch normalized() {
        String cleanStatus = status != null && ApplicationStatusLabels.STATUSES.contains(status) ? status : null;
        String cleanKeyword = ListFilters.cleanKeyword(keyword);
        if (cleanKeyword.isEmpty()) {
            cleanKeyword = null;
        }
        String cleanStage = Arrays.stream(ApplicationStage.values()).map(Enum::name)
                .filter(name -> name.equals(stage)).findFirst().orElse(null);
        return new ApplicationSearch(jobPostingId, cleanStatus, cleanKeyword, cleanStage);
    }

    /** Giai đoạn đang lọc; {@code null} nếu không lọc (gọi trên bản đã {@link #normalized()}). */
    public ApplicationStage stageFilter() {
        return stage == null ? null : ApplicationStage.valueOf(stage);
    }
}
