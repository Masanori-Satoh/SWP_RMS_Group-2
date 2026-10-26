package com.group2.rms.core.web;

/**
 * Thông tin người dùng hiển thị ở góc phải topbar (nội bộ và công khai).
 *
 * @param avatarUrl link xem ảnh đã tải lên ({@code /users/{id}/avatar?v=...}) hoặc {@code null} khi chưa có ảnh;
 *                  không phải giá trị thô của cột {@code AvatarUrl}
 */
public record TopbarUser(
    String fullName,
    String email,
    String roleName,
    String avatarUrl
) {}
