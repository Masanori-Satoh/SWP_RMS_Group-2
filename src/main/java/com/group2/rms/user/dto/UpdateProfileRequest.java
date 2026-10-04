package com.group2.rms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Editable profile fields.
 * Includes strict validation against invalid characters, code injection, and enforces real-name format.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required.")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters.")
        @Pattern(
                regexp = "^[\\p{L}][\\p{L}\\s.'-]*$",
                message = "Full name can only contain letters, spaces, hyphens, and apostrophes, and must start with a letter."
        )
        String fullName,

        @Size(max = 15, message = "Phone number cannot exceed 15 characters.")
        @Pattern(regexp = "^$|^[0-9+\\s()-]+$", message = "Phone number can only contain digits, spaces, hyphens, and plus signs.")
        String phoneNumber,

        @Size(max = 500, message = "Avatar URL cannot exceed 500 characters.")
        @Pattern(regexp = "^$|^https?://[^\\s<>\"]+$", message = "Avatar URL must be a valid URL starting with http:// or https://.")
        String avatarUrl) {
}
