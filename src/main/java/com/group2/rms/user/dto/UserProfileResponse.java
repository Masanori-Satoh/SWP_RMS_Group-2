package com.group2.rms.user.dto;

/** Read model for the "My Profile" screen. */
public record UserProfileResponse(
        Integer userId,
        String username,
        String email,
        String fullName,
        String phoneNumber,
        String avatarUrl,
        String roleName,
        String departmentName) {
}
