package com.group2.rms.user.controller;

import com.group2.rms.user.dto.UserAvatarResponse;
import com.group2.rms.user.service.UserProfileService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Duration;

/**
 * Ảnh đại diện đã tải lên, cho mọi người đã đăng nhập (rule {@code anyRequest().authenticated()} của
 * {@code SecurityConfig}). Link có tham số {@code v} đổi theo file nên được phép cache.
 */
@Controller
public class UserAvatarController {

    private final UserProfileService profileService;

    public UserAvatarController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/users/{id}/avatar")
    public ResponseEntity<Resource> avatar(@PathVariable Integer id) {
        UserAvatarResponse avatar = profileService.avatarFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
                .body(avatar.file());
    }
}
