package com.group2.rms.candidate.dto;

import com.group2.rms.candidate.service.ApplicationStatusLabels;

/**
 * Bộ lọc danh sách hồ sơ (query string của {@code GET /applications}).
 * Giá trị lạ không báo lỗi mà bị bỏ qua: đây là bộ lọc, không phải form nhập liệu.
 *
 * @param sort {@code score} (mặc định: điểm AI cao nhất) hoặc {@code newest}
 */
public record ApplicationSearch(Integer jobPostingId, String status, String keyword, String sort) {

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
        return new ApplicationSearch(jobPostingId, cleanStatus, cleanKeyword, cleanSort);
    }
}
