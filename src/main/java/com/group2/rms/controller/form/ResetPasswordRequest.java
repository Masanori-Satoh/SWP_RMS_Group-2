package com.group2.rms.controller.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class
ResetPasswordRequest {
    @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
    @Size(min = 8, max = 32, message = "Mật khẩu phải từ 8 đến 32 ký tự.")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu.")
    private String confirmPassword;
}
