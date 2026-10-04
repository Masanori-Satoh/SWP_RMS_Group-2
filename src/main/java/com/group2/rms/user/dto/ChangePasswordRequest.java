package com.group2.rms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Change password input model.
 * Passwords support all UTF-8 characters and special symbols (!@#$%^&*...).
 * Length is constrained strictly to 8-32 characters without restrictive character-set regexes.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required.")
        String currentPassword,

        @NotBlank(message = "New password is required.")
        @Size(min = 8, max = 32, message = "New password must be between 8 and 32 characters.")
        String newPassword,

        @NotBlank(message = "Confirm password is required.")
        String confirmPassword) {
}
