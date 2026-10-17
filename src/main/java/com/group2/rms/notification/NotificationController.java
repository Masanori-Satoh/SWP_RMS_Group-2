package com.group2.rms.notification;

import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Trang thông báo của người dùng hiện tại. Endpoint JSON cho chuông thông báo nằm ở {@link NotificationApiController}.
 */
@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        User user = currentUserService.requireUser();
        Page<NotificationResponse> result = notificationService.getNotificationsForUser(user, page, size);

        model.addAttribute("notifications", result.getContent());
        model.addAttribute("currentPage", result.getNumber() + 1);
        model.addAttribute("totalPages", Math.max(1, result.getTotalPages()));
        model.addAttribute("pageSize", result.getSize());
        model.addAttribute("totalElements", result.getTotalElements());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        model.addAttribute("activeMenu", "notifications");
        model.addAttribute("viewerName", user.getFullName());
        model.addAttribute("viewerRole", user.getRole() != null ? user.getRole().getRoleName() : "");

        return "notifications/list";
    }

    /**
     * Đánh dấu đã đọc. Khi {@code open=true} chuyển tới liên kết lưu trong thông báo (do server tạo),
     * không nhận URL chuyển hướng từ client.
     */
    @PostMapping("/{id}/read")
    public String markAsRead(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean open,
            RedirectAttributes flash) {
        NotificationResponse notification = notificationService.markAsRead(id, currentUserService.requireUser());
        if (open && notification.linkUrl() != null && notification.linkUrl().startsWith("/")
                && !notification.linkUrl().startsWith("//")) {
            return "redirect:" + notification.linkUrl();
        }
        flash.addFlashAttribute("successMessage", "Đã đánh dấu thông báo là đã đọc.");
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllAsRead(RedirectAttributes flash) {
        notificationService.markAllAsRead(currentUserService.requireUser());
        flash.addFlashAttribute("successMessage", "Đã đánh dấu tất cả thông báo là đã đọc.");
        return "redirect:/notifications";
    }
}
