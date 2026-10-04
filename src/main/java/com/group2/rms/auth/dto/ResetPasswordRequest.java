package com.group2.rms.auth.dto;

import com.group2.rms.core.validation.PasswordMatches;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PasswordMatches(message = "Passwords do not match.")
public class ResetPasswordRequest {

    @NotBlank(message = "Enter the 6-digit OTP code from your email.")
    @Pattern(regexp = "^\\d{6}$", message = "OTP code must be exactly 6 digits.")
    private String otp;
    @NotBlank(message = "Enter a new password.")
    @Size(min = 8, max = 32, message = "Password must contain 8–32 characters.")
    private String password;

    @NotBlank(message = "Confirm your password.")
    private String confirmPassword;
}
