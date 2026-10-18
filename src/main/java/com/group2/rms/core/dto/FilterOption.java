package com.group2.rms.core.dto;

/**
 * Một lựa chọn trong ô lọc của màn danh sách (dùng chung cho mọi màn danh sách có bộ lọc).
 *
 * @param value giá trị gửi lên URL (ví dụ "Pending_Director", "3")
 * @param label chữ tiếng Việt hiển thị cho người dùng (ví dụ "Chờ duyệt")
 */
public record FilterOption(String value, String label) {
}
