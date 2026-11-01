package com.group2.rms.notification.controller;

import com.group2.rms.notification.dto.NotificationResponse;
import com.group2.rms.notification.service.NotificationService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationApiControllerTests {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

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

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("tuan.vu", "pass", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        lenient().when(userRepository.findByUsernameIgnoreCase("tuan.vu")).thenReturn(Optional.of(testUser));
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
}
