package com.group2.rms.controller.form;

import com.group2.rms.service.CandidateRegistrationService.RegisterCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterAccountForm {
    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập username.")
    @Size(max = 50, message = "Username tối đa 50 ký tự.")
    private String username;

    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không đúng định dạng.")
    @Size(max = 150, message = "Email tối đa 150 ký tự.")
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @Size(min = 8, max = 32, message = "Mật khẩu phải từ 8 đến 32 ký tự.")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu.")
    private String confirmPassword;

    public RegisterCommand toCommand() {
        return new RegisterCommand(fullName, username, email, password);
    }
}
