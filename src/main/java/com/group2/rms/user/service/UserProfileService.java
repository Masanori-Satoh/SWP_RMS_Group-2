package com.group2.rms.user.service;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.user.dto.ChangePasswordRequest;
import com.group2.rms.user.dto.UpdateProfileRequest;
import com.group2.rms.user.dto.UserAvatarResponse;
import com.group2.rms.user.dto.UserProfileResponse;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.exception.AccountFieldException;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.validator.AvatarFileValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service quản lý thông tin tài khoản người dùng nội bộ và ứng viên.
 */
@Service
public class UserProfileService {

    static final String NO_DEPARTMENT = "Chưa thuộc phòng ban";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LocalAvatarStorage avatarStorage;
    private final AvatarFileValidator avatarValidator;
    private final UserAuditLogger userAuditLogger;

    public UserProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                              LocalAvatarStorage avatarStorage, AvatarFileValidator avatarValidator,
                              UserAuditLogger userAuditLogger) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarStorage = avatarStorage;
        this.avatarValidator = avatarValidator;
        this.userAuditLogger = userAuditLogger;
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
        updateProfile(findByUsername(username), request, null);
    }

    @Transactional
    public void updateCandidateProfile(String username, UpdateProfileRequest request, String email) {
        User user = findByUsername(username);
        if (!"Candidate".equals(user.getRole().getRoleName())) {
            throw new AccessDeniedException("Chỉ ứng viên được đổi email tại Hồ sơ cá nhân.");
        }
        updateProfile(user, request, email);
    }

    private void updateProfile(User user, UpdateProfileRequest request, String email) {
        boolean emailChanged = email != null && !email.equals(user.getEmail());
        if (emailChanged) {
            AccountManagementService.checkEmailAvailable(userRepository, email, user.getUserId());
        }
        var before = UserAuditLogger.userSnapshot(user);
        user.setFullName(request.fullName().trim());
        user.setPhoneNumber(blankToNull(request.phoneNumber()));
        if (emailChanged) {
            user.setEmail(email);
            try {
                userRepository.saveAndFlush(user);
            } catch (DataIntegrityViolationException e) {
                if (e.getMostSpecificCause().getMessage() != null
                        && e.getMostSpecificCause().getMessage().contains("UQ_User_Email")) {
                    throw new AccountFieldException("email", "Email này đã được sử dụng.");
                }
                throw e;
            }
        } else {
            userRepository.save(user);
        }
        userAuditLogger.changes("User", user.getUserId(), before, UserAuditLogger.userSnapshot(user));
    }

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
        userRepository.save(user);
    }

    public String avatarSrc(User user) {
        String key = LocalAvatarStorage.keyOf(user.getAvatarUrl());
        if (key == null || !avatarStorage.exists(key)) {
            return null;
        }
        String fileName = key.substring(key.lastIndexOf('/') + 1);
        return "/users/" + user.getUserId() + "/avatar?v=" + fileName.substring(0, fileName.lastIndexOf('.'));
    }

    @Transactional(readOnly = true)
    public UserAvatarResponse avatarFile(Integer userId) {
        String key = userRepository.findById(userId).map(u -> LocalAvatarStorage.keyOf(u.getAvatarUrl())).orElse(null);
        AvatarFileValidator.ImageType type = key == null ? null
                : AvatarFileValidator.ImageType.fromExtension(key.substring(key.lastIndexOf('.') + 1));
        if (type == null) {
            throw new ResourceNotFoundException("Không tìm thấy ảnh đại diện.");
        }
        return new UserAvatarResponse(avatarStorage.load(key), type.mediaType());
    }

    @Transactional
    public boolean changePassword(String username, ChangePasswordRequest request) {
        User user = findByUsername(username);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            return false;
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        userAuditLogger.record(user, "UPDATE", "User", user.getUserId(), null, "Đổi mật khẩu thành công");
        return true;
    }

    private User findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
    }

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
