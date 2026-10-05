package com.group2.rms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Editable profile fields.
 * Includes strict validation against invalid characters, code injection, and enforces real-name format.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Vui lòng nhập họ và tên.")
        @Size(min = 2, max = 100, message = "Họ và tên phải từ 2 đến 100 ký tự.")
        @Pattern(
                regexp = "^[\\p{L}][\\p{L}\\s.'-]*$",
                message = "Họ và tên phải bắt đầu bằng chữ cái và chỉ chứa chữ cái, khoảng trắng, dấu gạch nối và dấu nháy đơn."
        )
        String fullName,

        @Size(max = 15, message = "Số điện thoại không được vượt quá 15 ký tự.")
        @Pattern(regexp = "^$|^[0-9+\\s()-]+$", message = "Số điện thoại chỉ có thể chứa chữ số, khoảng trắng, dấu gạch nối và dấu cộng.")
        String phoneNumber,

        @Size(max = 500, message = "Đường dẫn ảnh đại diện không được vượt quá 500 ký tự.")
        @Pattern(regexp = "^$|^https?://[^\\s<>\"]+$", message = "Đường dẫn ảnh đại diện phải là URL hợp lệ bắt đầu bằng http:// hoặc https://.")
        String avatarUrl) {
}
