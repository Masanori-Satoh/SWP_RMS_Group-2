package com.group2.rms.candidate.portal.dto;

import com.group2.rms.user.dto.UserProfileResponse;

import java.time.LocalDate;

/**
 * Trang hồ sơ của ứng viên ({@code /portal/profile}): phần chung lấy từ {@code UserProfileService},
 * phần riêng từ bảng {@code Candidate}. Không chứa {@code CandidateSource}, {@code IsPotential}.
 */
public record CandidateProfileResponse(
        UserProfileResponse account,
        LocalDate dateOfBirth,
        String gender,
        String address,
        String linkedInUrl,
        String portfolioUrl) {
}
