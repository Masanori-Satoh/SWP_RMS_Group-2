package com.group2.rms.user.dto;

import com.group2.rms.core.validator.PasswordMatches;
import com.group2.rms.user.service.AccountManagementService.CreateCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PasswordMatches(message = "Mật khẩu xác nhận không khớp.")
public class CreateAccountRequest {

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 100, message = "Họ và tên không được quá 100 ký tự.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập tên đăng nhập.")
    @Size(max = 50, message = "Tên đăng nhập không được quá 50 ký tự.")
    private String username;

    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Vui lòng nhập email hợp lệ.")
    @Size(max = 150, message = "Email không được quá 150 ký tự.")
    private String email;

    @Size(max = 20, message = "Số điện thoại không được quá 20 ký tự.")
    @Pattern(regexp = "( *|0[0-9]{9})", message = "Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0.")
    private String phoneNumber;

    @NotNull(message = "Vui lòng chọn vai trò.")
    private Integer roleId;

    private Integer departmentId;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @Size(min = 8, max = 32, message = "Mật khẩu phải có từ 8 đến 32 ký tự.")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu.")
    private String confirmPassword;

    public CreateCommand toCommand() {
        return new CreateCommand(fullName, username, email, phoneNumber, roleId, departmentId, password);
    }
}
