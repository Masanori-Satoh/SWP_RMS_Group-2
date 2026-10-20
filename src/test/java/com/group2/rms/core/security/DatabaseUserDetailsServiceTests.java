package com.group2.rms.core.security;

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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTests {

    @Mock
    private UserRepository users;

    @InjectMocks
    private DatabaseUserDetailsService service;

    private User account;

    @BeforeEach
    void setUp() {
        account = User.builder()
                .userId(7)
                .username("candidate7")
                .email("candidate7@example.test")
                .passwordHash("stored-password-hash")
                .role(Role.builder().roleName("Candidate").build())
                .accountStatus("Active")
                .build();
    }

    @Test
    @DisplayName("AUTH-LOGIN-01 | Username tồn tại: trả đúng username, hash, quyền Candidate và trạng thái được đăng nhập")
    void loadUserByUsername_existingUsername_returnsLoginDetails() {
        // Given: Username tồn tại.
        when(users.findByUsernameIgnoreCase("candidate7")).thenReturn(Optional.of(account));

        // When: Gọi service.loadUserByUsername với dữ liệu đã chuẩn bị.
        UserDetails result = service.loadUserByUsername("candidate7");

        // Then: trả đúng username, hash, quyền Candidate và trạng thái được đăng nhập.
        assertNotNull(result);
        assertEquals("candidate7", result.getUsername());
        assertEquals("stored-password-hash", result.getPassword());
        assertEquals(true, result.isEnabled());
        assertEquals("ROLE_CANDIDATE", result.getAuthorities().iterator().next().getAuthority());
        verify(users, times(1)).findByUsernameIgnoreCase("candidate7");
        verify(users, times(1)).findByEmailIgnoreCase("candidate7");
    }

    @Test
    @DisplayName("AUTH-LOGIN-02 | Đăng nhập bằng email: trả username gốc của đúng tài khoản")
    void loadUserByUsername_existingEmail_returnsCanonicalUsername() {
        // Given: Đăng nhập bằng email.
        when(users.findByEmailIgnoreCase("candidate7@example.test")).thenReturn(Optional.of(account));

        // When: Gọi service.loadUserByUsername với dữ liệu đã chuẩn bị.
        UserDetails result = service.loadUserByUsername("candidate7@example.test");

        // Then: trả username gốc của đúng tài khoản.
        assertEquals("candidate7", result.getUsername());
        verify(users, times(1)).findByEmailIgnoreCase("candidate7@example.test");
    }

    @Test
    @DisplayName("AUTH-LOGIN-03 | Identifier có khoảng trắng đầu/cuối: bỏ khoảng trắng trước khi tìm tài khoản")
    void loadUserByUsername_identifierHasSpaces_trimsBeforeLookup() {
        // Given: Identifier có khoảng trắng đầu/cuối.
        when(users.findByUsernameIgnoreCase("candidate7")).thenReturn(Optional.of(account));

        // When: Gọi service.loadUserByUsername với dữ liệu đã chuẩn bị.
        UserDetails result = service.loadUserByUsername("  candidate7  ");

        // Then: bỏ khoảng trắng trước khi tìm tài khoản.
        assertEquals("candidate7", result.getUsername());
        verify(users, times(1)).findByUsernameIgnoreCase("candidate7");
        verify(users, times(1)).findByEmailIgnoreCase("candidate7");
    }

    @Test
    @DisplayName("AUTH-LOGIN-04 | Không tìm thấy username hoặc email: báo thông tin đăng nhập không hợp lệ")
    void loadUserByUsername_missingAccount_throwsException() {
        // Given: Không tìm thấy username hoặc email.
        when(users.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("missing")).thenReturn(Optional.empty());

        // When: Gọi service.loadUserByUsername với dữ liệu đã chuẩn bị.
        UsernameNotFoundException error = assertThrows(UsernameNotFoundException.class,
            () -> service.loadUserByUsername("missing"));

        // Then: báo thông tin đăng nhập không hợp lệ.
        assertEquals("Invalid credentials", error.getMessage());
        verify(users, times(1)).findByUsernameIgnoreCase("missing");
    }

    @Test
    @DisplayName("AUTH-LOGIN-05 | Identifier trùng username và email của hai tài khoản khác nhau: từ chối đăng nhập")
    void loadUserByUsername_identifierBelongsToTwoAccounts_rejectsAmbiguousLogin() {
        // Given: Identifier trùng username và email của hai tài khoản khác nhau.
        User another = User.builder().userId(8).build();
        when(users.findByUsernameIgnoreCase("shared@example.test")).thenReturn(Optional.of(account));
        when(users.findByEmailIgnoreCase("shared@example.test")).thenReturn(Optional.of(another));

        // When: Gọi service.loadUserByUsername với dữ liệu đã chuẩn bị.
        UsernameNotFoundException error = assertThrows(UsernameNotFoundException.class,
            () -> service.loadUserByUsername("shared@example.test"));

        // Then: từ chối đăng nhập.
        assertEquals("Invalid credentials", error.getMessage());
    }

    @Test
    @DisplayName("AUTH-LOGIN-06 | Username và email cùng trỏ một tài khoản: không từ chối nhầm vì trùng identifier")
    void loadUserByUsername_bothLookupsReturnSameAccount_allowsLogin() {
        // Given: Username và email cùng trỏ một tài khoản.
        when(users.findByUsernameIgnoreCase("candidate7")).thenReturn(Optional.of(account));
        when(users.findByEmailIgnoreCase("candidate7")).thenReturn(Optional.of(account));

        // When: Gọi service.loadUserByUsername với dữ liệu đã chuẩn bị.
        UserDetails result = service.loadUserByUsername("candidate7");

        // Then: không từ chối nhầm vì trùng identifier.
        assertEquals("candidate7", result.getUsername());
        assertEquals(true, result.isEnabled());
    }

    @Test
    @DisplayName("AUTH-LOGIN-07 | Tài khoản Inactive: trả UserDetails bị vô hiệu hóa để Security chặn đăng nhập")
    void loadUserByUsername_inactiveAccount_returnsDisabledDetails() {
        // Given: Tài khoản Inactive.
        account.setAccountStatus("Inactive");
        when(users.findByUsernameIgnoreCase("candidate7")).thenReturn(Optional.of(account));

        // When: Gọi service.loadUserByUsername với dữ liệu đã chuẩn bị.
        UserDetails result = service.loadUserByUsername("candidate7");

        // Then: trả UserDetails bị vô hiệu hóa để Security chặn đăng nhập.
        assertEquals(false, result.isEnabled());
    }

    @Test
    @DisplayName("AUTH-LOGIN-08 | Tài khoản Blocked: trả UserDetails bị vô hiệu hóa để Security chặn đăng nhập")
    void loadUserByUsername_blockedAccount_returnsDisabledDetails() {
        // Given: Tài khoản Blocked.
        account.setAccountStatus("Blocked");
        when(users.findByUsernameIgnoreCase("candidate7")).thenReturn(Optional.of(account));

        // When: Gọi service.loadUserByUsername với dữ liệu đã chuẩn bị.
        UserDetails result = service.loadUserByUsername("candidate7");

        // Then: trả UserDetails bị vô hiệu hóa để Security chặn đăng nhập.
        assertEquals(false, result.isEnabled());
    }

    @Test
    @DisplayName("AUTH-LOGIN-09 | Role không được hỗ trợ: từ chối tạo thông tin đăng nhập")
    void loadUserByUsername_unknownRole_throwsException() {
        // Given: Role không được hỗ trợ.
        account.setRole(Role.builder().roleName("Unsupported").build());
        when(users.findByUsernameIgnoreCase("candidate7")).thenReturn(Optional.of(account));

        // When: Gọi service.loadUserByUsername với dữ liệu đã chuẩn bị.
        UsernameNotFoundException error = assertThrows(UsernameNotFoundException.class,
            () -> service.loadUserByUsername("candidate7"));

        // Then: từ chối tạo thông tin đăng nhập.
        assertEquals("Invalid credentials", error.getMessage());
    }

    @Test
    @DisplayName("AUTH-LOGIN-10 | Tài khoản thiếu role: từ chối tạo thông tin đăng nhập")
    void loadUserByUsername_missingRole_throwsException() {
        // Given: Tài khoản thiếu role.
        account.setRole(null);
        when(users.findByUsernameIgnoreCase("candidate7")).thenReturn(Optional.of(account));

        // When: Gọi service.loadUserByUsername với dữ liệu đã chuẩn bị.
        UsernameNotFoundException error = assertThrows(UsernameNotFoundException.class,
            () -> service.loadUserByUsername("candidate7"));

        // Then: từ chối tạo thông tin đăng nhập.
        assertEquals("Invalid credentials", error.getMessage());
    }

    @Test
    @DisplayName("AUTH-LOGIN-11 | Identifier null: ném lỗi, không gọi repository tìm tài khoản")
    void loadUserByUsername_nullIdentifier_doesNotQueryRepository() {
        // Given: Không có username/email được truyền vào.
        // When: Yêu cầu nạp thông tin đăng nhập với identifier null.
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername(null));

        // Then: Chặn đầu vào trước khi truy vấn tài khoản.
        verify(users, times(0)).findByUsernameIgnoreCase(anyString());
        verify(users, times(0)).findByEmailIgnoreCase(anyString());
    }

    @Test
    @DisplayName("AUTH-LOGIN-12 | Identifier chỉ chứa khoảng trắng: ném lỗi, không gọi repository")
    void loadUserByUsername_blankIdentifier_doesNotQueryRepository() {
        // Given: Identifier chỉ chứa khoảng trắng.
        // When: Gọi service.loadUserByUsername với dữ liệu đã chuẩn bị.
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("   "));
        // Then: ném lỗi, không gọi repository.
        verify(users, times(0)).findByUsernameIgnoreCase(anyString());
        verify(users, times(0)).findByEmailIgnoreCase(anyString());
    }
}
