package com.group2.rms.candidate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO yêu cầu cập nhật thông tin hồ sơ của ứng viên.
 */
public record UpdateCandidateProfileRequest(
        @Past @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,

        @Size(max = 20) String gender,

        @Size(max = 255) String address,

        @Size(max = 500) @Pattern(regexp = "^$|^https?://[^\\s<>\"]+$", message = "Link LinkedIn phải bắt đầu bằng http:// hoặc https://.") String linkedInUrl,

        @Size(max = 500) @Pattern(regexp = "^$|^https?://[^\\s<>\"]+$", message = "Link Portfolio phải bắt đầu bằng http:// hoặc https://.") String portfolioUrl,

        @Email(message = "Vui lòng nhập email hợp lệ.") @Size(max = 150, message = "Email không được quá 150 ký tự.") @Pattern(regexp = ".+", message = "Vui lòng nhập email.") String email) {

    public UpdateCandidateProfileRequest {
        email = (email == null) ? null : email.strip();
        address = (address == null || address.isBlank()) ? null : address.strip();
        linkedInUrl = (linkedInUrl == null || linkedInUrl.isBlank()) ? null : linkedInUrl.strip();
        portfolioUrl = (portfolioUrl == null || portfolioUrl.isBlank()) ? null : portfolioUrl.strip();
    }

    public static final List<String> GENDERS = List.of("Nam", "Nữ", "Khác");

    public static List<String> genderOptions(String current) {
        if (current == null || current.isBlank() || GENDERS.contains(current)) {
            return GENDERS;
        }
        List<String> options = new ArrayList<>(GENDERS);
        options.add(current);
        return List.copyOf(options);
    }
}
