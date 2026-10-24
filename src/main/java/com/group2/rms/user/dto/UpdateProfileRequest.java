package com.group2.rms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Editable profile fields.
 * Includes strict validation against invalid characters, code injection, and enforces real-name format.
 * Trên form, câu báo của {@code @NotBlank}, {@code @Size} lấy từ {@code messages.properties} của nhóm (đã là tiếng Việt).
 * Ảnh đại diện không nằm ở đây: chỉ đổi bằng file tải lên ({@code UserProfileService.changeAvatar}).
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Vui lòng nhập họ tên.")
        @Size(min = 2, max = 100, message = "Họ tên dài từ 2 đến 100 ký tự.")
        @Pattern(
                regexp = "^[\\p{L}][\\p{L}\\s.'-]*$",
                message = "Họ tên bắt đầu bằng chữ cái, chỉ gồm chữ cái, khoảng trắng, dấu chấm, dấu gạch nối và dấu nháy."
        )
        String fullName,

        @Size(max = 15, message = "Số điện thoại tối đa 15 ký tự.")
        @Pattern(regexp = "^$|^[0-9+\\s()-]+$", message = "Số điện thoại chỉ gồm chữ số, khoảng trắng, dấu gạch nối, dấu cộng và ngoặc.")
        String phoneNumber) {
}
