package com.group2.rms.core.security;

import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentUserServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final CurrentUserService service = new CurrentUserService(users);

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    @DisplayName("SEC-USER-01 | Ném AccessDeniedException và không truy vấn tài khoản")
    void requireUser_noAuthentication_rejectsBeforeLookup() {
        // Given: Chưa có Authentication trong SecurityContext.
        SecurityContextHolder.clearContext();
        // When: Gọi requireUser khi chưa đăng nhập.
        AccessDeniedException error = assertThrows(AccessDeniedException.class, service::requireUser);
        // Then: Ném AccessDeniedException và không truy vấn tài khoản.
        assertEquals("Please log in.", error.getMessage());
        verify(users, times(0)).findByUsernameIgnoreCase(any());
    }

    @Test
    @DisplayName("SEC-USER-02 | Từ chối và không truy vấn tài khoản")
    void requireUser_unauthenticatedToken_rejectsBeforeLookup() {
        // Given: Token chưa được xác thực.
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated("employee", "secret"));
        // When: Gọi requireUser bằng token chưa xác thực.
        assertThrows(AccessDeniedException.class, service::requireUser);
        // Then: Từ chối và không truy vấn tài khoản.
        verify(users, times(0)).findByUsernameIgnoreCase(any());
    }

    @Test
    @DisplayName("SEC-USER-03 | Trả đúng account; truy vấn một lần; không lưu")
    void requireUser_activeAccount_returnsRepositoryAccount() {
        // Given: Token đã xác thực và account Active tồn tại.
        User account = User.builder().userId(7).accountStatus("Active").build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("employee", "unused", List.of()));
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(account));
        // When: Lấy user hiện tại.
        User result = service.requireUser();
        // Then: Trả đúng account; truy vấn một lần; không lưu.
        assertEquals(account, result);
        verify(users, times(1)).findByUsernameIgnoreCase("employee");
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("SEC-USER-04 | Tài khoản nullStatus bị từ chối, không lưu")
    void requireUser_nonActiveAccount_rejectsWithoutSaving_nullStatus() {
        // Given: Token đã xác thực nhưng trạng thái account là null.
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("employee", "unused", List.of()));
        when(users.findByUsernameIgnoreCase("employee"))
                .thenReturn(Optional.of(User.builder().accountStatus(null).build()));
        // When: Gọi requireUser.
        AccessDeniedException error = assertThrows(AccessDeniedException.class, service::requireUser);
        // Then: Ném lỗi account không Active và không lưu.
        assertEquals("An active account is required.", error.getMessage());
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("SEC-USER-05 | Tài khoản emptyStatus bị từ chối, không lưu")
    void requireUser_nonActiveAccount_rejectsWithoutSaving_emptyStatus() {
        // Given: Token đã xác thực nhưng trạng thái account là "".
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("employee", "unused", List.of()));
        when(users.findByUsernameIgnoreCase("employee"))
                .thenReturn(Optional.of(User.builder().accountStatus("").build()));
        // When: Gọi requireUser.
        AccessDeniedException error = assertThrows(AccessDeniedException.class, service::requireUser);
        // Then: Ném lỗi account không Active và không lưu.
        assertEquals("An active account is required.", error.getMessage());
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("SEC-USER-06 | Tài khoản inactive bị từ chối, không lưu")
    void requireUser_nonActiveAccount_rejectsWithoutSaving_inactive() {
        // Given: Token đã xác thực nhưng trạng thái account là "Inactive".
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("employee", "unused", List.of()));
        when(users.findByUsernameIgnoreCase("employee"))
                .thenReturn(Optional.of(User.builder().accountStatus("Inactive").build()));
        // When: Gọi requireUser.
        AccessDeniedException error = assertThrows(AccessDeniedException.class, service::requireUser);
        // Then: Ném lỗi account không Active và không lưu.
        assertEquals("An active account is required.", error.getMessage());
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("SEC-USER-07 | Tài khoản blocked bị từ chối, không lưu")
    void requireUser_nonActiveAccount_rejectsWithoutSaving_blocked() {
        // Given: Token đã xác thực nhưng trạng thái account là "Blocked".
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("employee", "unused", List.of()));
        when(users.findByUsernameIgnoreCase("employee"))
                .thenReturn(Optional.of(User.builder().accountStatus("Blocked").build()));
        // When: Gọi requireUser.
        AccessDeniedException error = assertThrows(AccessDeniedException.class, service::requireUser);
        // Then: Ném lỗi account không Active và không lưu.
        assertEquals("An active account is required.", error.getMessage());
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("SEC-USER-08 | Từ chối và không lưu account")
    void requireUser_removedAccount_rejectsWithoutSaving() {
        // Given: Token hợp lệ nhưng account đã bị xóa.
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("employee", "unused", List.of()));
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.empty());
        // When: Lấy user hiện tại.
        assertThrows(AccessDeniedException.class, service::requireUser);
        // Then: Từ chối và không lưu account.
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("SEC-USER-09 | Thiếu/sai role trả false, role trùng trả true")
    void hasRole_matchesCurrentRoleAndRejectsMissingOrDifferentRole() {
        // Given: User lần lượt chưa có role và có role Hiring Manager.
        User user = User.builder().build();
        // When: Kiểm tra role HR và Hiring Manager với các trạng thái role đã chuẩn bị.
        // Then: Role thiếu hoặc khác trả false; role trùng trả true.
        assertEquals(false, CurrentUserService.hasRole(user, "HR"));
        user.setRole(Role.builder().roleName("Hiring Manager").build());
        assertEquals(false, CurrentUserService.hasRole(user, "HR"));
        assertEquals(true, CurrentUserService.hasRole(user, "Hiring Manager"));
    }

}

