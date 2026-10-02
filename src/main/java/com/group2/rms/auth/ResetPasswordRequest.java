package com.group2.rms.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
<<<<<<< Updated upstream:src/main/java/com/group2/rms/auth/ResetPasswordRequest.java
public class
ResetPasswordRequest {
    @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
    @Size(min = 8, max = 32, message = "Mật khẩu phải từ 8 đến 32 ký tự.")
=======
public class ResetPasswordForm {
    @NotBlank(message = "Enter a new password.")
    @Size(min = 8, max = 32, message = "Password must contain 8–32 characters.")
>>>>>>> Stashed changes:src/main/java/com/group2/rms/controller/form/ResetPasswordForm.java
    private String password;

    @NotBlank(message = "Confirm your password.")
    private String confirmPassword;
}
