package com.group2.rms.interview.dto;

/**
 * Nhãn trạng thái phụ trên thẻ lịch phỏng vấn (kết quả cuối, hoặc trạng thái chấm điểm của người xem).
 *
 * @param code    mã dùng cho bộ lọc phía client (data-result / data-evaluation)
 * @param display chữ hiển thị
 * @param badge   class badge trong catalog
 */
public record InterviewCardState(String code, String display, String badge) {
}
