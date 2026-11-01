package com.group2.rms.notification.controller;

import com.group2.rms.notification.dto.NotificationResponse;
import com.group2.rms.notification.service.NotificationService;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/*
 * API CONTROLLER: JSON cho chuông thông báo trên topbar (số chưa đọc, danh sách gần đây, đánh dấu đã đọc).
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Vui lòng đăng nhập để tiếp tục.");
        }
        return userRepository.findByUsernameIgnoreCase(auth.getName())
            .filter(u -> "Active".equals(u.getAccountStatus()))
            .orElseThrow(() -> new AccessDeniedException("Tài khoản không hợp lệ hoặc đã bị khóa."));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        try {
            User user = currentUser();
            long count = notificationService.getUnreadCount(user);
            return ResponseEntity.ok(Map.of("unreadCount", count));
        } catch (Exception e) {
            log.error("Failed to fetch unread notification count: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("unreadCount", 0));
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentNotifications() {
        try {
            User user = currentUser();
            Page<NotificationResponse> result = notificationService.getNotificationsForUser(user, 1, 5);
            long unreadCount = notificationService.getUnreadCount(user);
            return ResponseEntity.ok(Map.of(
                "unreadCount", unreadCount,
                "notifications", result.getContent()
            ));
        } catch (Exception e) {
            log.error("Failed to fetch recent notifications: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("unreadCount", 0, "notifications", List.of()));
        }
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        try {
            User user = currentUser();
            notificationService.markAllAsRead(user);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false));
        }
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        try {
            User user = currentUser();
            notificationService.markAsRead(id, user);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false));
        }
    }
}
