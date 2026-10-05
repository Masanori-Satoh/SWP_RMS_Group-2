package com.group2.rms.user.service;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.user.dto.ChangePasswordRequest;
import com.group2.rms.user.dto.UpdateProfileRequest;
import com.group2.rms.user.dto.UserProfileResponse;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Self-service profile operations. Always operates on the authenticated account only. */
@Service
public class UserProfileService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        User user = findByUsername(username);
        return new UserProfileResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getRole().getRoleName(),
                user.getDepartment() == null ? "N/A" : user.getDepartment().getDepartmentName());
    }

    @Transactional
    public void updateProfile(String username, UpdateProfileRequest request) {
        User user = findByUsername(username);
        user.setFullName(request.fullName().trim());
        user.setPhoneNumber(blankToNull(request.phoneNumber()));
        user.setAvatarUrl(blankToNull(request.avatarUrl()));
        users.save(user);
    }

    /**
     * @return {@code false} when the current password is incorrect (nothing is changed).
     */
    @Transactional
    public boolean changePassword(String username, ChangePasswordRequest request) {
        User user = findByUsername(username);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            return false;
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        users.save(user);
        return true;
    }

    private User findByUsername(String username) {
        return users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng."));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
