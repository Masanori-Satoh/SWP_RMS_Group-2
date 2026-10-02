package com.group2.rms.auth;

import com.group2.rms.auth.CandidateRegistrationService.RegisterCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
<<<<<<< Updated upstream:src/main/java/com/group2/rms/auth/RegisterAccountRequest.java
public class RegisterAccountRequest {
    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự.")
=======
public class RegisterAccountForm {
    @NotBlank(message = "Enter a full name.")
    @Size(max = 100, message = "Full name must be at most 100 characters.")
>>>>>>> Stashed changes:src/main/java/com/group2/rms/controller/form/RegisterAccountForm.java
    private String fullName;

    @NotBlank(message = "Enter a username.")
    @Size(max = 50, message = "Username must be at most 50 characters.")
    private String username;

    @NotBlank(message = "Enter an email address.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 150, message = "Email must be at most 150 characters.")
    private String email;

    @NotBlank(message = "Enter a password.")
    @Size(min = 8, max = 32, message = "Password must contain 8–32 characters.")
    private String password;

    @NotBlank(message = "Confirm your password.")
    private String confirmPassword;

    public RegisterCommand toCommand() {
        return new RegisterCommand(fullName, username, email, password);
    }
}
