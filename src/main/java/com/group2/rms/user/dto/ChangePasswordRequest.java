package com.group2.rms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Change password input model.
 * Passwords support all UTF-8 characters and special symbols (!@#$%^&*...).
 * Length is constrained strictly to 8-32 characters without restrictive character-set regexes.
 * Câu báo ở đây là câu JSON trả về hộp thoại đổi mật khẩu ({@code FieldError.getDefaultMessage()}).
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại.")
        String currentPassword,

        @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
        @Size(min = 8, max = 32, message = "Mật khẩu mới dài 8–32 ký tự.")
        String newPassword,

        @NotBlank(message = "Vui lòng nhập lại mật khẩu mới.")
        String confirmPassword) {
}
