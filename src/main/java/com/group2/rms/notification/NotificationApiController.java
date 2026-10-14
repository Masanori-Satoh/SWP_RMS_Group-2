package com.group2.rms.notification;

import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
        User user = currentUser();
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentNotifications() {
        User user = currentUser();
        Page<NotificationResponse> result = notificationService.getNotificationsForUser(user, 1, 5);
        long unreadCount = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(Map.of(
                "unreadCount", unreadCount,
                "notifications", result.getContent()
        ));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        User user = currentUser();
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        User user = currentUser();
        notificationService.markAsRead(id, user);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
