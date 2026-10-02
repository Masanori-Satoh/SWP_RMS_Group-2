package com.group2.rms.auth;

import com.group2.rms.auth.CandidateRegistrationService.RegisterCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterAccountRequest {
    @NotBlank(message = "Enter a full name.")
    @Size(max = 100, message = "Full name must be at most 100 characters.")
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
