package com.group2.rms.core.web;

import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Nguồn dữ liệu duy nhất cho khối định danh ở topbar.
 * Mọi trang (dashboard, profile, job-board, job-detail...) đều nhận cùng một model attribute
 * {@code topbarUser}, thay vì để fragment tự đoán từ biến riêng của từng controller.
 * Ảnh: chỉ ảnh đã tải lên và có file trên máy này mới thành link; còn lại {@code null} để topbar hiện chữ cái đầu.
 * {@code UserProfileService} lấy qua {@link ObjectProvider}: advice này được nạp vào mọi test {@code @WebMvcTest},
 * phần lớn không có bean đó; thiếu thì coi như chưa có ảnh thay vì làm hỏng context.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class TopbarUserAdvice {

    private final UserRepository userRepository;
    private final ObjectProvider<UserProfileService> profileService;

    @ModelAttribute("topbarUser")
    public TopbarUser topbarUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        UserProfileService profiles = profileService.getIfAvailable();
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
                .map(u -> new TopbarUser(u.getFullName(), u.getEmail(),
                        u.getRole() != null ? u.getRole().getRoleName() : null,
                        profiles != null ? profiles.avatarSrc(u) : null))
                .orElse(null);
    }
}
