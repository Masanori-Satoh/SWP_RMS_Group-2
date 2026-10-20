package com.group2.rms.candidate.dto;

import java.util.List;
import java.util.Map;

/**
 * Trang My Applications: các thẻ đơn (đã lọc theo bước nếu có, mới cập nhật nhất trước) và số đơn ở từng bước cho
 * dải hướng dẫn 4 bước (bấm một bước = lọc theo bước đó).
 *
 * @param applications các thẻ đơn sau khi lọc
 * @param stepCounts   số đơn ở mỗi bước 1–4 (tính trên mọi đơn, không phụ thuộc bộ lọc); bước không có đơn là 0
 * @param selectedStep bước đang lọc (1–4); {@code null} nếu xem tất cả
 * @param total        tổng số đơn, không phụ thuộc bộ lọc
 */
public record MyApplicationListResponse(
        List<MyApplicationResponse> applications,
        Map<Integer, Long> stepCounts,
        Integer selectedStep,
        int total) {
}
