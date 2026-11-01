package com.group2.rms.notification.controller;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.notification.dto.NotificationResponse;
import com.group2.rms.notification.service.NotificationService;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/*
 * API CONTROLLER: JSON cho chuông thông báo trên topbar (số chưa đọc, danh sách gần đây, đánh dấu đã đọc).
 * Lỗi trả đúng mã HTTP kèm JSON (không phải trang lỗi HTML); topbar-notifications.js giữ số cũ khi !res.ok.
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        User user = currentUserService.requireUser();
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(user)));
    }

    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentNotifications() {
        User user = currentUserService.requireUser();
        Page<NotificationResponse> result = notificationService.getNotificationsForUser(user, 1, 5);
        return ResponseEntity.ok(Map.of(
            "unreadCount", notificationService.getUnreadCount(user),
            "notifications", result.getContent()
        ));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        notificationService.markAllAsRead(currentUserService.requireUser());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id, currentUserService.requireUser());
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ----- Lỗi: ưu tiên hơn GlobalExceptionHandler (vốn trả trang HTML) cho riêng controller này -----

    /** Chưa đăng nhập hoặc tài khoản không còn Active. */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return failure(HttpStatus.FORBIDDEN, "Tài khoản không hợp lệ hoặc đã bị khóa.");
    }

    /** Thông báo không tồn tại hoặc không thuộc về người đang đăng nhập. */
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return failure(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Notification API failed: {}", ex.getMessage(), ex);
        return failure(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tải thông báo lúc này.");
    }

    private static ResponseEntity<Map<String, Object>> failure(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("success", false, "message", message));
    }
}
