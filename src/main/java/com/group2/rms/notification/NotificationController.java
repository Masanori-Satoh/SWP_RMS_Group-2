package com.group2.rms.notification;

import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

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

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        User user = currentUser();
        Page<NotificationResponse> result = notificationService.getNotificationsForUser(user, page, size);
        long unreadCount = notificationService.getUnreadCount(user);

        model.addAttribute("notifications", result.getContent());
        model.addAttribute("currentPage", result.getNumber() + 1);
        model.addAttribute("totalPages", Math.max(1, result.getTotalPages()));
        model.addAttribute("pageSize", result.getSize());
        model.addAttribute("totalElements", result.getTotalElements());
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("activeMenu", "notifications");
        model.addAttribute("viewerName", user.getFullName());
        model.addAttribute("viewerRole", user.getRole() != null ? user.getRole().getRoleName() : "");

        return "notifications/list";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String redirectUrl,
            RedirectAttributes flash) {
        User user = currentUser();
        notificationService.markAsRead(id, user);
        flash.addFlashAttribute("successMessage", "Đã đánh dấu thông báo là đã đọc.");

        if (redirectUrl != null && !redirectUrl.isBlank() && redirectUrl.startsWith("/")) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllAsRead(RedirectAttributes flash) {
        User user = currentUser();
        notificationService.markAllAsRead(user);
        flash.addFlashAttribute("successMessage", "Đã đánh dấu tất cả thông báo là đã đọc.");
        return "redirect:/notifications";
    }

    @GetMapping("/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        try {
            User user = currentUser();
            long count = notificationService.getUnreadCount(user);
            return ResponseEntity.ok(Map.of("unreadCount", count));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("unreadCount", 0));
        }
    }
}
