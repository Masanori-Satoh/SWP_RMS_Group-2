package com.group2.rms.core.web;

import java.time.LocalDateTime;

/**
 * Một mốc trên timeline, hiển thị bằng fragment {@code fragments/ui/timeline :: timeline(items)}.
 * Chỉ là dữ liệu hiển thị: mỗi module tự dựng danh sách mốc từ bảng của mình.
 *
 * @param actor người thực hiện; {@code null} nếu không rõ
 * @param body  nội dung thêm (nhận xét...); {@code null} nếu không có
 * @param tone  màu chấm: {@code neutral}, {@code brand}, {@code success}, {@code warning}, {@code danger}
 */
public record TimelineItem(LocalDateTime at, String title, String actor, String body, String tone) {
}
