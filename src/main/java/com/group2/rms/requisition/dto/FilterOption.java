package com.group2.rms.requisition.dto;

/**
 * Một lựa chọn trong ô lọc của màn danh sách.
 *
 * @param value giá trị gửi lên URL (ví dụ "Pending_Director", "3")
 * @param label chữ tiếng Việt hiển thị cho người dùng (ví dụ "Chờ duyệt")
 */
public record FilterOption(String value, String label) {
}
