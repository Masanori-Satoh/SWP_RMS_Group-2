package com.group2.rms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Editable profile fields.
 * Same name and phone limits as registration and Admin account forms (SRS, User schema).
 * Ảnh đại diện không nằm ở đây: chỉ đổi bằng file tải lên ({@code UserProfileService.changeAvatar}).
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Vui lòng nhập họ và tên.")
        @Size(max = 100, message = "Họ và tên không được quá 100 ký tự.")
        String fullName,

        @Size(max = 20, message = "Số điện thoại không được quá 20 ký tự.")
        String phoneNumber) {
}
