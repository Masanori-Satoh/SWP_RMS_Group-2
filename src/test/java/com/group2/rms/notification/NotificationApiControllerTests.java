package com.group2.rms.notification;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * CONTROLLER TEST (@WebMvcTest): NotificationApiController (JSON cho chuông thông báo trên topbar)
 */
@WebMvcTest(controllers = NotificationApiController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class NotificationApiControllerTests {

    private static final String BASE = "/api/notifications";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private NotificationService notificationService;
    @MockitoBean
    private UserRepository userRepository;

    private User manager;

    @BeforeEach
    void setUp() {
        manager = account("hm_user", "Active");
    }

    // ----- Hàm hỗ trợ -----

    private User account(String username, String accountStatus) {
        User user = User.builder().userId(username.hashCode()).username(username).fullName("Tên " + username)
                .accountStatus(accountStatus).role(Role.builder().roleName("Hiring Manager").build()).build();
        when(userRepository.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(user));
        return user;
    }

    private static NotificationResponse notification(long id) {
        return new NotificationResponse(id, "Tin tuyển dụng đã phát hành: Java", "Nội dung", "JOB_POSTING_PUBLISHED",
                "50", "/internal/job-postings/50", false, LocalDateTime.of(2026, 10, 18, 9, 0), null);
    }

    // =========================================================================
    // GET /api/notifications/unread-count
    // =========================================================================

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Có 3 thông báo chưa đọc -> Trả {unreadCount: 3}")
    void getUnreadCount_shouldReturnCount_whenUserIsActive() throws Exception {
        // Arrange
        when(notificationService.getUnreadCount(manager)).thenReturn(3L);

        // Act + Assert
        mvc.perform(get(BASE + "/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Service lỗi -> Vẫn trả 200 với unreadCount = 0 (chuông không làm hỏng trang)")
    void getUnreadCount_shouldReturnZero_whenServiceFails() throws Exception {
        // Arrange
        when(notificationService.getUnreadCount(manager)).thenThrow(new IllegalStateException("DB down"));

        // Act + Assert
        mvc.perform(get(BASE + "/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    @WithMockUser(username = "locked_user", roles = "HIRING_MANAGER")
    @DisplayName("Tài khoản bị khóa -> Bộ lọc phiên đăng xuất, chuyển về đăng nhập, không đọc thông báo")
    void getUnreadCount_shouldLogOutWithoutQuery_whenAccountIsLocked() throws Exception {
        // Arrange
        account("locked_user", "Locked");

        // Act + Assert
        mvc.perform(get(BASE + "/unread-count"))
                .andExpect(redirectedUrl("/login?session-expired"));
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Chưa đăng nhập -> Không trả dữ liệu (chuyển về đăng nhập), không gọi service")
    void getUnreadCount_shouldNotCallService_whenNotLoggedIn() throws Exception {
        // Act + Assert
        mvc.perform(get(BASE + "/unread-count"))
                .andExpect(status().is3xxRedirection());
        verifyNoInteractions(notificationService);
    }

    // =========================================================================
    // GET /api/notifications/recent
    // =========================================================================

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Mở chuông -> Lấy đúng 5 thông báo mới nhất (trang 1, cỡ 5) kèm số chưa đọc")
    void getRecentNotifications_shouldReturnTopFiveAndCount_whenUserIsActive() throws Exception {
        // Arrange
        when(notificationService.getNotificationsForUser(manager, 1, 5))
                .thenReturn(new PageImpl<>(List.of(notification(7L))));
        when(notificationService.getUnreadCount(manager)).thenReturn(1L);

        // Act + Assert
        mvc.perform(get(BASE + "/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.notifications.length()").value(1))
                .andExpect(jsonPath("$.notifications[0].notificationId").value(7))
                .andExpect(jsonPath("$.notifications[0].title").value("Tin tuyển dụng đã phát hành: Java"))
                .andExpect(jsonPath("$.notifications[0].linkUrl").value("/internal/job-postings/50"))
                .andExpect(jsonPath("$.notifications[0].isRead").value(false));
        verify(notificationService).getNotificationsForUser(manager, 1, 5);
    }

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Service lỗi -> Trả 200 với unreadCount = 0 và danh sách rỗng")
    void getRecentNotifications_shouldReturnEmpty_whenServiceFails() throws Exception {
        // Arrange
        when(notificationService.getNotificationsForUser(manager, 1, 5)).thenThrow(new IllegalStateException("DB down"));

        // Act + Assert
        mvc.perform(get(BASE + "/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andExpect(jsonPath("$.notifications.length()").value(0));
    }

    // =========================================================================
    // POST /api/notifications/{id}/read và /read-all
    // =========================================================================

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Đánh dấu một thông báo của mình -> {success: true}, gọi service đúng id và người xem")
    void markAsRead_shouldReturnSuccess_whenNotificationIsOwned() throws Exception {
        // Arrange
        when(notificationService.markAsRead(15L, manager)).thenReturn(notification(15L));

        // Act + Assert
        mvc.perform(post(BASE + "/15/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(notificationService).markAsRead(15L, manager);
    }

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Đánh dấu thông báo của người khác / không tồn tại -> 400 {success: false}")
    void markAsRead_shouldReturnBadRequest_whenNotificationIsNotOwned() throws Exception {
        // Arrange
        when(notificationService.markAsRead(99L, manager))
                .thenThrow(new ResourceNotFoundException("Thông báo không tồn tại hoặc không thuộc về bạn."));

        // Act + Assert
        mvc.perform(post(BASE + "/99/read"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Đánh dấu tất cả -> {success: true}, gọi service cho đúng người xem")
    void markAllAsRead_shouldReturnSuccess_whenUserIsActive() throws Exception {
        // Act + Assert
        mvc.perform(post(BASE + "/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(notificationService).markAllAsRead(manager);
    }

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Đánh dấu tất cả nhưng service lỗi -> 400 {success: false}")
    void markAllAsRead_shouldReturnBadRequest_whenServiceFails() throws Exception {
        // Arrange
        doThrow(new IllegalStateException("DB down")).when(notificationService).markAllAsRead(manager);

        // Act + Assert
        mvc.perform(post(BASE + "/read-all"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
