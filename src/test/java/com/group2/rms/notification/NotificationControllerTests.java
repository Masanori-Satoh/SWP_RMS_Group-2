package com.group2.rms.notification;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * CONTROLLER TEST (@WebMvcTest): NotificationController (trang /notifications)
 */
@WebMvcTest(controllers = NotificationController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class NotificationControllerTests {

    private static final String BASE = "/notifications";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private NotificationService notificationService;
    @MockitoBean
    private CurrentUserService currentUserService;
    // Bộ lọc phiên (AccountSessionGuardFilter) và topbar đọc tài khoản ở mỗi request
    @MockitoBean
    private UserRepository userRepository;

    private User manager;

    @BeforeEach
    void setUp() {
        manager = User.builder().userId(10).username("hm_user").fullName("Trần Thị Quản Lý").accountStatus("Active")
                .role(Role.builder().roleName("Hiring Manager").build()).build();
        when(userRepository.findByUsernameIgnoreCase("hm_user")).thenReturn(Optional.of(manager));
        when(currentUserService.requireUser()).thenReturn(manager);
    }

    // ----- Hàm hỗ trợ -----

    private static NotificationResponse notification(long id, boolean read, String linkUrl) {
        return new NotificationResponse(id, "Yêu cầu tuyển dụng đã bị từ chối: Senior Java", "Lý do: thiếu ngân sách.",
                "REQUISITION_REJECTED", "101", linkUrl, read, LocalDateTime.of(2026, 10, 18, 9, 15), null);
    }

    private void givenNotifications(List<NotificationResponse> rows, long unread) {
        Page<NotificationResponse> page = new PageImpl<>(rows, PageRequest.of(0, 10), rows.size());
        when(notificationService.getNotificationsForUser(manager, 1, 10)).thenReturn(page);
        when(notificationService.getUnreadCount(manager)).thenReturn(unread);
    }

    // =========================================================================
    // GET /notifications
    // =========================================================================

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Mở trang thông báo -> Hiện danh sách của chính mình, số chưa đọc, nút 'Xem chi tiết'")
    void list_shouldRenderOwnNotifications_whenUserIsLoggedIn() throws Exception {
        // Arrange
        givenNotifications(List.of(notification(1L, false, "/requisitions/101")), 1);

        // Act + Assert
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications/list"))
                .andExpect(model().attribute("unreadCount", 1L))
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(model().attribute("totalPages", 1))
                .andExpect(model().attribute("totalElements", 1L))
                .andExpect(model().attribute("activeMenu", "notifications"))
                .andExpect(content().string(containsString("Bạn có 1 thông báo chưa đọc")))
                .andExpect(content().string(containsString("Yêu cầu tuyển dụng đã bị từ chối: Senior Java")))
                .andExpect(content().string(containsString("btn-read-all")))
                .andExpect(content().string(containsString("Xem chi tiết")));
    }

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Chưa có thông báo nào -> Trạng thái rỗng, tổng số trang vẫn là 1, không có nút đọc tất cả")
    void list_shouldShowEmptyState_whenUserHasNoNotifications() throws Exception {
        // Arrange
        givenNotifications(List.of(), 0);

        // Act + Assert
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalPages", 1))
                .andExpect(content().string(containsString("Không có thông báo nào")))
                // Chuông trên topbar cũng có chữ "Đánh dấu tất cả đã đọc" nên kiểm tra theo class nút của trang
                .andExpect(content().string(not(containsString("btn-read-all"))));
    }

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Gửi ?page=&size= -> Chuyển nguyên cho service (service tự chỉnh về khoảng hợp lệ)")
    void list_shouldPassPagingToService_whenQueryParamsAreGiven() throws Exception {
        // Arrange
        when(notificationService.getNotificationsForUser(manager, 3, 999)).thenReturn(Page.empty());

        // Act
        mvc.perform(get(BASE).param("page", "3").param("size", "999"))
                .andExpect(status().isOk());

        // Assert
        verify(notificationService).getNotificationsForUser(manager, 3, 999);
    }

    @Test
    @DisplayName("Chưa đăng nhập -> Chuyển về trang đăng nhập, không gọi service")
    void list_shouldRedirectToLogin_whenNotLoggedIn() throws Exception {
        // Act + Assert
        mvc.perform(get(BASE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        verifyNoInteractions(notificationService);
    }

    // =========================================================================
    // POST /notifications/{id}/read
    // =========================================================================

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Bấm 'Đánh dấu đã đọc' -> Về lại trang thông báo kèm câu báo thành công")
    void markAsRead_shouldRedirectBackWithFlash_whenOpenIsNotRequested() throws Exception {
        // Arrange
        when(notificationService.markAsRead(1L, manager)).thenReturn(notification(1L, true, "/requisitions/101"));

        // Act + Assert
        mvc.perform(post(BASE + "/1/read").with(csrf()))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("successMessage", "Đã đánh dấu thông báo là đã đọc."));
        verify(notificationService).markAsRead(1L, manager);
    }

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Bấm 'Xem chi tiết' với link nội bộ -> Đánh dấu đã đọc rồi chuyển tới đúng trang đích")
    void markAsRead_shouldRedirectToLink_whenOpenIsRequestedAndLinkIsInternal() throws Exception {
        // Arrange
        when(notificationService.markAsRead(1L, manager)).thenReturn(notification(1L, true, "/requisitions/101"));

        // Act + Assert
        mvc.perform(post(BASE + "/1/read").param("open", "true").with(csrf()))
                .andExpect(redirectedUrl("/requisitions/101"));
    }

    @ParameterizedTest(name = "link = {0}")
    @ValueSource(strings = {"//evil.example.com/phish", "https://evil.example.com", "javascript:alert(1)", "requisitions/101"})
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Link không phải đường dẫn nội bộ '/...' -> Không chuyển ra ngoài, về lại trang thông báo (chặn open redirect)")
    void markAsRead_shouldStayOnNotifications_whenLinkIsNotInternal(String linkUrl) throws Exception {
        // Arrange
        when(notificationService.markAsRead(1L, manager)).thenReturn(notification(1L, true, linkUrl));

        // Act + Assert
        mvc.perform(post(BASE + "/1/read").param("open", "true").with(csrf()))
                .andExpect(redirectedUrl(BASE));
    }

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Bấm 'Xem chi tiết' nhưng thông báo không có link -> Về lại trang thông báo")
    void markAsRead_shouldStayOnNotifications_whenLinkIsMissing() throws Exception {
        // Arrange
        when(notificationService.markAsRead(1L, manager)).thenReturn(notification(1L, true, null));

        // Act + Assert
        mvc.perform(post(BASE + "/1/read").param("open", "true").with(csrf()))
                .andExpect(redirectedUrl(BASE));
    }

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Đánh dấu thông báo của người khác / id không tồn tại -> 404")
    void markAsRead_shouldReturn404_whenNotificationIsNotOwned() throws Exception {
        // Arrange
        when(notificationService.markAsRead(99L, manager))
                .thenThrow(new ResourceNotFoundException("Thông báo không tồn tại hoặc không thuộc về bạn."));

        // Act + Assert
        mvc.perform(post(BASE + "/99/read").param("open", "true").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Đánh dấu đã đọc thiếu CSRF token -> 403, không gọi service")
    void markAsRead_shouldReturn403_whenCsrfTokenIsMissing() throws Exception {
        // Act + Assert
        mvc.perform(post(BASE + "/1/read"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(notificationService);
    }

    // =========================================================================
    // POST /notifications/read-all
    // =========================================================================

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Bấm 'Đánh dấu tất cả đã đọc' -> Gọi service cho đúng người, về lại trang kèm câu báo")
    void markAllAsRead_shouldRedirectBackWithFlash_whenRequested() throws Exception {
        // Act + Assert
        mvc.perform(post(BASE + "/read-all").with(csrf()))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("successMessage", "Đã đánh dấu tất cả thông báo là đã đọc."));
        verify(notificationService).markAllAsRead(manager);
    }

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("Đánh dấu tất cả thiếu CSRF token -> 403, không gọi service")
    void markAllAsRead_shouldReturn403_whenCsrfTokenIsMissing() throws Exception {
        // Act + Assert
        mvc.perform(post(BASE + "/read-all"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Chưa đăng nhập mà gửi đánh dấu tất cả -> Không gọi service")
    void markAllAsRead_shouldNotCallService_whenNotLoggedIn() throws Exception {
        // Act
        mvc.perform(post(BASE + "/read-all").with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Assert
        verifyNoInteractions(notificationService);
        verify(currentUserService, never()).requireUser();
    }
}
