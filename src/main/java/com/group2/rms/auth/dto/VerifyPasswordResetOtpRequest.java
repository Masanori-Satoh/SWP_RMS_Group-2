package com.group2.rms.auth.dto;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyPasswordResetOtpRequest {

    @NotBlank(message = "Enter the verification code.")
    @Pattern(regexp = "[0-9]{6}", message = "OTP code must be exactly 6 digits.")
    private String otp;
}
