package com.group2.rms.user.dto;

/**
 * Read model for the "My Profile" screen.
 *
 * @param avatarUrl link xem ảnh ({@code /users/{id}/avatar?v=...}) hoặc {@code null} khi chưa có ảnh tải lên;
 *                  không phải giá trị thô của cột {@code AvatarUrl}
 * @param roleName  tên vai trò gốc (dùng chọn sidebar, so sánh quyền)
 * @param roleLabel tên vai trò tiếng Việt để hiện trên trang
 */
public record UserProfileResponse(
        Integer userId,
        String username,
        String email,
        String fullName,
        String phoneNumber,
        String avatarUrl,
        String roleName,
        String roleLabel,
        String departmentName) {
}
