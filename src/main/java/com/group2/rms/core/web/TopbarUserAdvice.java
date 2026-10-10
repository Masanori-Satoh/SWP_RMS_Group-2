package com.group2.rms.core.web;

import com.group2.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Nguồn dữ liệu duy nhất cho khối định danh ở topbar.
 * Mọi trang (dashboard, profile, job-board, job-detail...) đều nhận cùng một model attribute
 * {@code topbarUser}, thay vì để fragment tự đoán từ biến riêng của từng controller.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class TopbarUserAdvice {

    private final UserRepository userRepository;

    @ModelAttribute("topbarUser")
    public TopbarUser topbarUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
                .map(u -> new TopbarUser(u.getFullName(), u.getEmail(),
                        u.getRole() != null ? u.getRole().getRoleName() : null, u.getAvatarUrl()))
                .orElse(null);
    }
}
