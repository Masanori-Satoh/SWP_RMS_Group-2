package com.group2.rms.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
<<<<<<< Updated upstream:src/main/java/com/group2/rms/auth/ForgotPasswordRequest.java
public class ForgotPasswordRequest {
    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không đúng định dạng.")
    @Size(max = 150, message = "Email tối đa 150 ký tự.")
=======
public class ForgotPasswordForm {
    @NotBlank(message = "Enter an email address.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 150, message = "Email must be at most 150 characters.")
>>>>>>> Stashed changes:src/main/java/com/group2/rms/controller/form/ForgotPasswordForm.java
    private String email;
}
