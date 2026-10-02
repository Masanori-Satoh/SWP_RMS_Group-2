package com.group2.rms.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    @NotBlank(message = "Enter a new password.")
    @Size(min = 8, max = 32, message = "Password must contain 8–32 characters.")
    private String password;

    @NotBlank(message = "Confirm your password.")
    private String confirmPassword;
}
