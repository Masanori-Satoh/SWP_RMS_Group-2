package com.group2.rms.notification.controller;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.notification.dto.NotificationResponse;
import com.group2.rms.notification.service.NotificationService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationApiControllerTests {

    @Mock
    private NotificationService notificationService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private NotificationApiController notificationApiController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1)
                .username("tuan.vu")
                .fullName("Vũ Anh Tuấn")
                .accountStatus("Active")
                .role(Role.builder().roleId(1).roleName("Hiring Manager").build())
                .build();

        lenient().when(currentUserService.requireUser()).thenReturn(testUser);
    }

    @Test
    @DisplayName("getUnreadCount returns correct unread count")
    void getUnreadCount_success() {
        when(notificationService.getUnreadCount(testUser)).thenReturn(3L);

        ResponseEntity<Map<String, Object>> response = notificationApiController.getUnreadCount();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("unreadCount")).isEqualTo(3L);
    }

    @Test
    @DisplayName("getRecentNotifications returns top 5 notifications and unread count")
    void getRecentNotifications_success() {
        NotificationResponse item = new NotificationResponse(
                10L,
                "REQ-101 đã bị từ chối",
                "Lý do: xem lại lương",
                "REQUISITION_REJECTED",
                "101",
                "/requisitions/101",
                false,
                LocalDateTime.now(),
                null
        );

        when(notificationService.getNotificationsForUser(eq(testUser), eq(1), eq(5)))
                .thenReturn(new PageImpl<>(List.of(item)));
        when(notificationService.getUnreadCount(testUser)).thenReturn(1L);

        ResponseEntity<Map<String, Object>> response = notificationApiController.getRecentNotifications();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("unreadCount")).isEqualTo(1L);
        @SuppressWarnings("unchecked")
        List<NotificationResponse> list = (List<NotificationResponse>) response.getBody().get("notifications");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).title()).isEqualTo("REQ-101 đã bị từ chối");
    }

    @Test
    @DisplayName("markAllAsRead calls notificationService.markAllAsRead")
    void markAllAsRead_success() {
        doNothing().when(notificationService).markAllAsRead(testUser);

        ResponseEntity<Map<String, Object>> response = notificationApiController.markAllAsRead();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
        verify(notificationService).markAllAsRead(testUser);
    }

    @Test
    @DisplayName("markAsRead calls notificationService.markAsRead with target ID")
    void markAsRead_success() {
        when(notificationService.markAsRead(eq(15L), eq(testUser))).thenReturn(null);

        ResponseEntity<Map<String, Object>> response = notificationApiController.markAsRead(15L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
        verify(notificationService).markAsRead(15L, testUser);
    }

    @Test
    @DisplayName("Inactive or anonymous viewer gets 403 JSON instead of a fake zero count")
    void getUnreadCount_shouldReturn403_whenViewerNotActive() throws Exception {
        when(currentUserService.requireUser()).thenThrow(new AccessDeniedException("An active account is required."));

        mockMvc().perform(get("/api/notifications/unread-count"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.unreadCount").doesNotExist());
        verify(notificationService, never()).getUnreadCount(any());
    }

    @Test
    @DisplayName("Marking someone else's notification returns 404 JSON")
    void markAsRead_shouldReturn404_whenNotificationNotOwned() throws Exception {
        when(notificationService.markAsRead(99L, testUser))
                .thenThrow(new ResourceNotFoundException("Thông báo không tồn tại hoặc không thuộc về bạn."));

        mockMvc().perform(post("/api/notifications/99/read"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Thông báo không tồn tại hoặc không thuộc về bạn."));
    }

    @Test
    @DisplayName("Unexpected failure returns 500 JSON so the bell keeps its last count")
    void getRecentNotifications_shouldReturn500_whenServiceFails() throws Exception {
        when(notificationService.getNotificationsForUser(testUser, 1, 5)).thenThrow(new IllegalStateException("db down"));

        mockMvc().perform(get("/api/notifications/recent"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(notificationApiController).build();
    }
}
