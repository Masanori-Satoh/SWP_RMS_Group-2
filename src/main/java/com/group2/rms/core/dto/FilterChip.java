package com.group2.rms.core.dto;

import java.util.List;

/**
 * Một "chip" hiển thị bộ lọc đang bật, ví dụ "Trạng thái: Chờ duyệt ✕" (dùng chung cho mọi màn danh sách).
 *
 * @param label  chữ hiển thị trên chip
 * @param params tên các tham số URL cần bỏ khi bấm ✕ (khoảng ngày gồm 2 tham số, ví dụ createdFrom, createdTo)
 */
public record FilterChip(String label, List<String> params) {
}
