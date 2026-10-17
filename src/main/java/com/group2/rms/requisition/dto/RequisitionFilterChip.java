package com.group2.rms.requisition.dto;

import java.util.List;

/**
 * Một "chip" hiển thị bộ lọc đang bật, ví dụ "Trạng thái: Chờ duyệt ✕".
 *
 * @param label  chữ hiển thị trên chip
 * @param params tên các tham số URL cần bỏ khi bấm ✕ (ngày tạo gồm 2 tham số createdFrom, createdTo)
 */
public record RequisitionFilterChip(String label, List<String> params) {
}
