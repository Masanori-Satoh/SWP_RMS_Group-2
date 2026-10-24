package com.group2.rms.user.service;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.user.dto.ChangePasswordRequest;
import com.group2.rms.user.dto.UpdateProfileRequest;
import com.group2.rms.user.dto.UserAvatarResponse;
import com.group2.rms.user.dto.UserProfileResponse;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.validator.AvatarFileValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Self-service profile operations. Always operates on the authenticated account only.
 * Nguồn duy nhất cho phần chung (bảng {@code [User]}) của mọi vai trò; trang hồ sơ ứng viên gọi lại service này.
 */
@Service
public class UserProfileService {

    static final String NO_DEPARTMENT = "Chưa thuộc phòng ban";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final LocalAvatarStorage avatarStorage;
    private final AvatarFileValidator avatarValidator;

    public UserProfileService(UserRepository users, PasswordEncoder passwordEncoder,
                              LocalAvatarStorage avatarStorage, AvatarFileValidator avatarValidator) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.avatarStorage = avatarStorage;
        this.avatarValidator = avatarValidator;
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
                avatarSrc(user),
                user.getRole().getRoleName(),
                roleLabel(user.getRole().getRoleName()),
                user.getDepartment() == null ? NO_DEPARTMENT : user.getDepartment().getDepartmentName());
    }

    @Transactional
    public void updateProfile(String username, UpdateProfileRequest request) {
        User user = findByUsername(username);
        user.setFullName(request.fullName().trim());
        user.setPhoneNumber(blankToNull(request.phoneNumber()));
        users.save(user);
    }

    /**
     * Thay ảnh đại diện bằng file tải lên. Mỗi người một file: commit xong thì xóa file cũ;
     * transaction không commit thì xóa file vừa lưu (file nằm ngoài DB nên rollback không xóa được).
     */
    @Transactional
    public void changeAvatar(String username, MultipartFile file) {
        User user = findByUsername(username);
        AvatarFileValidator.ImageType type = avatarValidator.validate(file);
        String oldKey = LocalAvatarStorage.keyOf(user.getAvatarUrl());
        String newKey = avatarStorage.store(file, user.getUserId(), type.extension());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    avatarStorage.delete(newKey);
                } else if (oldKey != null) {
                    avatarStorage.delete(oldKey);
                }
            }
        });
        user.setAvatarUrl(LocalAvatarStorage.URL_PREFIX + newKey);
        users.save(user);
    }

    /**
     * Link xem ảnh của một người, có tham số {@code v} đổi theo file để trình duyệt tải lại khi đổi ảnh;
     * {@code null} khi chưa có ảnh tải lên hoặc máy này không có file (giao diện hiện chữ cái đầu).
     */
    public String avatarSrc(User user) {
        String key = LocalAvatarStorage.keyOf(user.getAvatarUrl());
        if (key == null || !avatarStorage.exists(key)) {
            return null;
        }
        String fileName = key.substring(key.lastIndexOf('/') + 1);
        return "/users/" + user.getUserId() + "/avatar?v=" + fileName.substring(0, fileName.lastIndexOf('.'));
    }

    /** File ảnh của một người cho {@code /users/{id}/avatar}; không có ảnh thì ném {@code ResourceNotFoundException}. */
    @Transactional(readOnly = true)
    public UserAvatarResponse avatarFile(Integer userId) {
        String key = users.findById(userId).map(u -> LocalAvatarStorage.keyOf(u.getAvatarUrl())).orElse(null);
        AvatarFileValidator.ImageType type = key == null ? null
                : AvatarFileValidator.ImageType.fromExtension(key.substring(key.lastIndexOf('.') + 1));
        if (type == null) {
            throw new ResourceNotFoundException("Không tìm thấy ảnh đại diện.");
        }
        return new UserAvatarResponse(avatarStorage.load(key), type.mediaType());
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
    }

    /**
     * Tên vai trò tiếng Việt. Cùng bảng tên với {@code fragments/layout/topbar-user.html} (topbar tự dịch bằng th:with);
     * đổi tên ở đây thì đổi cả ở đó.
     */
    static String roleLabel(String roleName) {
        if (roleName == null) {
            return null;
        }
        return switch (roleName) {
            case "Hiring Manager" -> "Trưởng phòng tuyển dụng";
            case "HR" -> "Nhân sự";
            case "HR Specialist" -> "Chuyên viên Nhân sự";
            case "Director" -> "Giám đốc";
            case "System Admin" -> "Quản trị hệ thống";
            case "Interviewer" -> "Người phỏng vấn";
            case "Candidate" -> "Ứng viên";
            default -> roleName;
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
