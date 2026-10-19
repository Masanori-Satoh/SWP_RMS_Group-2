package com.group2.rms.auth.service;

import com.group2.rms.auth.dto.AccountSnapshotResponse;
import com.group2.rms.auth.exception.PasswordRecoveryFlowException;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTests {

    private static final Instant NOW = Instant.parse("2026-10-19T02:00:00Z");
    private static final Instant EXPIRES_AT = NOW.plusSeconds(900);
    private static final String EMAIL = "candidate7@example.test";

    @Mock
    private UserRepository users;

    @Mock
    private PasswordEncoder encoder;

    private PasswordResetService service;
    private User account;
    private AccountSnapshotResponse snapshot;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new PasswordResetService(users, encoder, clock);
        account = User.builder().userId(7).email(EMAIL)
                .passwordHash("old-hash").accountStatus("Active").build();
        snapshot = new AccountSnapshotResponse(7, EMAIL, "old-hash");
    }

    @Test
    @DisplayName("AUTH-RESET-01 | Email Active tồn tại: tạo snapshot chứa đúng ID, email và hash hiện tại")
    void findActiveAccount_existingEmail_returnsSnapshot() {
        // Given
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account));

        // When
        AccountSnapshotResponse result = service.findActiveAccount(EMAIL);

        // Then
        assertNotNull(result);
        assertEquals(7, result.userId());
        assertEquals(EMAIL, result.email());
        assertEquals("old-hash", result.passwordHash());
        verify(users, times(1)).findByEmailIgnoreCase(EMAIL);
    }

    @Test
    @DisplayName("AUTH-RESET-02 | Email có khoảng trắng đầu/cuối: tìm bằng email đã trim")
    void findActiveAccount_emailHasSpaces_trimsBeforeLookup() {
        // Given
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account));

        // When
        AccountSnapshotResponse result = service.findActiveAccount("  " + EMAIL + "  ");

        // Then
        assertEquals(EMAIL, result.email());
        verify(users, times(1)).findByEmailIgnoreCase(EMAIL);
    }

    @Test
    @DisplayName("AUTH-RESET-03 | Email null: báo EMAIL_REQUIRED, không truy vấn tài khoản")
    void findActiveAccount_nullEmail_throwsRequiredError() {
        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.findActiveAccount(null));

        // Then
        assertEquals("EMAIL_REQUIRED", error.getErrorCode());
        verify(users, times(0)).findByEmailIgnoreCase(anyString());
    }

    @Test
    @DisplayName("AUTH-RESET-04 | Email chỉ chứa khoảng trắng: báo EMAIL_REQUIRED")
    void findActiveAccount_blankEmail_throwsRequiredError() {
        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.findActiveAccount("   "));

        // Then
        assertEquals("EMAIL_REQUIRED", error.getErrorCode());
    }

    @Test
    @DisplayName("AUTH-RESET-05 | Email không tồn tại: báo EMAIL_NOT_FOUND tại field email")
    void findActiveAccount_missingEmail_throwsNotFoundError() {
        // Given
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.findActiveAccount(EMAIL));

        // Then
        assertEquals("EMAIL_NOT_FOUND", error.getErrorCode());
        assertEquals("email", error.getField());
    }

    @Test
    @DisplayName("AUTH-RESET-06 | Email thuộc tài khoản Inactive: không cho khởi tạo reset")
    void findActiveAccount_inactiveAccount_throwsInactiveError() {
        // Given
        account.setAccountStatus("Inactive");
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account));

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.findActiveAccount(EMAIL));

        // Then
        assertEquals("ACCOUNT_NOT_ACTIVE", error.getErrorCode());
    }

    @Test
    @DisplayName("AUTH-RESET-07 | Email thuộc tài khoản Blocked: không cho khởi tạo reset")
    void findActiveAccount_blockedAccount_throwsInactiveError() {
        // Given
        account.setAccountStatus("Blocked");
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account));

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.findActiveAccount(EMAIL));

        // Then
        assertEquals("ACCOUNT_NOT_ACTIVE", error.getErrorCode());
    }

    @Test
    @DisplayName("AUTH-RESET-08 | Snapshot khớp tài khoản Active: xác nhận yêu cầu reset còn hợp lệ")
    void isCurrent_unchangedAccount_returnsTrue() {
        // Given
        when(users.findById(7)).thenReturn(Optional.of(account));

        // When
        boolean result = service.isCurrent(snapshot);

        // Then
        assertEquals(true, result);
        verify(users, times(1)).findById(7);
    }

    @Test
    @DisplayName("AUTH-RESET-09 | Kiểm tra snapshot null: trả false và không truy vấn tài khoản")
    void isCurrent_nullSnapshot_returnsFalse() {
        // When
        boolean result = service.isCurrent(null);

        // Then
        assertEquals(false, result);
        verify(users, times(0)).findById(any());
    }

    @Test
    @DisplayName("AUTH-RESET-10 | Tài khoản trong snapshot đã bị xóa: yêu cầu reset không còn hợp lệ")
    void isCurrent_removedAccount_returnsFalse() {
        // Given
        when(users.findById(7)).thenReturn(Optional.empty());

        // When
        boolean result = service.isCurrent(snapshot);

        // Then
        assertEquals(false, result);
    }

    @Test
    @DisplayName("AUTH-RESET-11 | Email tài khoản đã đổi sau khi tạo snapshot: trả false")
    void isCurrent_emailChanged_returnsFalse() {
        // Given
        account.setEmail("changed@example.test");
        when(users.findById(7)).thenReturn(Optional.of(account));

        // When
        boolean result = service.isCurrent(snapshot);

        // Then
        assertEquals(false, result);
    }

    @Test
    @DisplayName("AUTH-RESET-12 | Hash mật khẩu đã đổi sau khi tạo snapshot: trả false")
    void isCurrent_passwordChanged_returnsFalse() {
        // Given
        account.setPasswordHash("different-hash");
        when(users.findById(7)).thenReturn(Optional.of(account));

        // When
        boolean result = service.isCurrent(snapshot);

        // Then
        assertEquals(false, result);
    }

    @Test
    @DisplayName("AUTH-RESET-13 | Tài khoản bị vô hiệu hóa sau khi tạo snapshot: trả false")
    void isCurrent_accountDeactivated_returnsFalse() {
        // Given
        account.setAccountStatus("Inactive");
        when(users.findById(7)).thenReturn(Optional.of(account));

        // When
        boolean result = service.isCurrent(snapshot);

        // Then
        assertEquals(false, result);
    }

    @Test
    @DisplayName("AUTH-RESET-14 | Snapshot hợp lệ và còn hạn: mã hóa mật khẩu mới, lưu đúng tài khoản một lần")
    void changePassword_validSnapshot_encodesAndSavesPassword() {
        // Given
        when(users.findByIdForUpdate(7)).thenReturn(Optional.of(account));
        when(encoder.encode("NewPass123!")).thenReturn("new-hash");

        // When
        service.changePassword(snapshot, "NewPass123!", EXPIRES_AT);

        // Then
        assertEquals("new-hash", account.getPasswordHash());
        assertEquals(EMAIL, account.getEmail());
        assertEquals("Active", account.getAccountStatus());
        verify(users, times(1)).findByIdForUpdate(7);
        verify(encoder, times(1)).encode("NewPass123!");
        verify(users, times(1)).saveAndFlush(account);
    }

    @Test
    @DisplayName("AUTH-RESET-15 | Yêu cầu đã quá hạn: ném lỗi, giữ hash cũ và không lưu")
    void changePassword_expiredRequest_doesNotSave() {
        // Given
        when(users.findByIdForUpdate(7)).thenReturn(Optional.of(account));

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.changePassword(snapshot, "NewPass123!", NOW.minusSeconds(1)));

        // Then
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        assertEquals("old-hash", account.getPasswordHash());
        verify(users, times(0)).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("AUTH-RESET-16 | Thời gian hiện tại đúng bằng hạn reset: coi là hết hạn và không lưu")
    void changePassword_atExpiryBoundary_doesNotSave() {
        // Given
        when(users.findByIdForUpdate(7)).thenReturn(Optional.of(account));

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.changePassword(snapshot, "NewPass123!", NOW));

        // Then
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        verify(encoder, times(0)).encode(anyString());
        verify(users, times(0)).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("AUTH-RESET-17 | Mật khẩu đã được đổi bởi yêu cầu khác: không ghi đè bằng snapshot cũ")
    void changePassword_stalePasswordSnapshot_doesNotOverwriteNewerPassword() {
        // Given
        account.setPasswordHash("changed-by-another-request");
        when(users.findByIdForUpdate(7)).thenReturn(Optional.of(account));

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.changePassword(snapshot, "NewPass123!", EXPIRES_AT));

        // Then
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        assertEquals("changed-by-another-request", account.getPasswordHash());
        verify(users, times(0)).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("AUTH-RESET-18 | Tài khoản không còn tồn tại khi lưu: ném lỗi và không ghi mật khẩu")
    void changePassword_missingAccount_doesNotSave() {
        // Given
        when(users.findByIdForUpdate(7)).thenReturn(Optional.empty());

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.changePassword(snapshot, "NewPass123!", EXPIRES_AT));

        // Then
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        verify(encoder, times(0)).encode(anyString());
        verify(users, times(0)).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("AUTH-RESET-19 | Mật khẩu ngắn hơn 8 ký tự: báo PASSWORD_INVALID trước khi truy vấn DB")
    void changePassword_shortPassword_returnsPasswordFieldError() {
        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.changePassword(snapshot, "short", EXPIRES_AT));

        // Then
        assertEquals("PASSWORD_INVALID", error.getErrorCode());
        assertEquals("password", error.getField());
        verify(users, times(0)).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("AUTH-RESET-20 | Mật khẩu dài hơn 32 ký tự: từ chối và không lưu")
    void changePassword_passwordOver32Characters_doesNotSave() {
        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.changePassword(snapshot, "a".repeat(33), EXPIRES_AT));

        // Then
        assertEquals("PASSWORD_INVALID", error.getErrorCode());
        assertEquals("password", error.getField());
        verify(users, times(0)).findByIdForUpdate(any());
        verify(encoder, times(0)).encode(anyString());
        verify(users, times(0)).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("AUTH-RESET-21 | Thiếu snapshot tài khoản: báo RESET_REQUEST_INVALID trước khi truy vấn hoặc lưu DB")
    void changePassword_nullSnapshot_rejectsRequest() {
        // Given: snapshot null nên không xác định được tài khoản được phép reset.
        // Mật khẩu mới và thời hạn vẫn hợp lệ để chỉ kiểm tra đúng lỗi thiếu snapshot.

        // When: Gọi đổi mật khẩu mà không có snapshot.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.changePassword(null, "NewPass123!", EXPIRES_AT));

        // Then: Từ chối yêu cầu; không tìm/khóa tài khoản và không lưu mật khẩu.
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        verify(users, times(0)).findByIdForUpdate(any());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(anyString());
    }

    @Test
    @DisplayName("AUTH-RESET-22 | Thiếu thời hạn reset: báo RESET_REQUEST_INVALID trước khi truy vấn hoặc lưu DB")
    void changePassword_missingExpiry_rejectsRequest() {
        // Given: Snapshot và mật khẩu hợp lệ, nhưng expiresAt null.
        // Không có thời hạn thì không thể xác nhận yêu cầu reset còn hiệu lực.

        // When: Gọi đổi mật khẩu mà không truyền thời hạn.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.changePassword(snapshot, "NewPass123!", null));

        // Then: Từ chối yêu cầu trước khi tìm/khóa tài khoản hoặc thay đổi mật khẩu.
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        verify(users, times(0)).findByIdForUpdate(any());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(anyString());
    }
    @Test
    @DisplayName("AUTH-RESET-23 | Password null: báo PASSWORD_INVALID, không tìm tài khoản hoặc lưu")
    void changePassword_nullPassword_rejectsBeforeLookup() {
        // Given: Snapshot và thời hạn hợp lệ, chỉ password bị thiếu.
        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.changePassword(snapshot, null, EXPIRES_AT));

        // Then
        assertEquals("PASSWORD_INVALID", error.getErrorCode());
        assertEquals("password", error.getField());
        verify(users, times(0)).findByIdForUpdate(any());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
    }

    @Test
    @DisplayName("AUTH-RESET-24 | Password gồm 8 dấu cách: vẫn từ chối dù đủ độ dài tối thiểu")
    void changePassword_blankPassword_rejectsBeforeLookup() {
        // Given: Đủ 8 ký tự nhưng tất cả đều là khoảng trắng.
        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.changePassword(snapshot, "        ", EXPIRES_AT));

        // Then
        assertEquals("PASSWORD_INVALID", error.getErrorCode());
        verify(users, times(0)).findByIdForUpdate(any());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(anyString());
    }

    @Test
    @DisplayName("AUTH-RESET-25 | Password đúng 8 ký tự: chấp nhận, mã hóa và lưu một lần")
    void changePassword_exactlyEightCharacters_savesPassword() {
        // Given
        when(users.findByIdForUpdate(7)).thenReturn(Optional.of(account));
        when(encoder.encode("Abc123!x")).thenReturn("minimum-hash");

        // When
        service.changePassword(snapshot, "Abc123!x", EXPIRES_AT);

        // Then
        assertEquals("minimum-hash", account.getPasswordHash());
        verify(encoder, times(1)).encode("Abc123!x");
        verify(users, times(1)).saveAndFlush(account);
    }

    @Test
    @DisplayName("AUTH-RESET-26 | Password đúng 32 ký tự: chấp nhận tại biên độ dài tối đa")
    void changePassword_exactlyThirtyTwoCharacters_savesPassword() {
        // Given
        String password = "a".repeat(32);
        when(users.findByIdForUpdate(7)).thenReturn(Optional.of(account));
        when(encoder.encode(password)).thenReturn("maximum-hash");

        // When
        service.changePassword(snapshot, password, EXPIRES_AT);

        // Then
        assertEquals("maximum-hash", account.getPasswordHash());
        verify(encoder, times(1)).encode(password);
        verify(users, times(1)).saveAndFlush(account);
    }

    @Test
    @DisplayName("AUTH-RESET-27 | Email đổi ngay trước khi lưu: kiểm tra lại snapshot và không đổi password")
    void changePassword_emailChangedBeforeSave_rejectsStaleSnapshot() {
        // Given: Tài khoản đọc dưới khóa đã có email mới.
        account.setEmail("changed@example.test");
        when(users.findByIdForUpdate(7)).thenReturn(Optional.of(account));

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.changePassword(snapshot, "NewPass123!", EXPIRES_AT));

        // Then
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        assertEquals("changed@example.test", account.getEmail());
        assertEquals("old-hash", account.getPasswordHash());
        verify(encoder, times(0)).encode(anyString());
        verify(users, times(0)).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("AUTH-RESET-28 | Account bị Blocked ngay trước khi lưu: giữ hash cũ và không lưu")
    void changePassword_accountBlockedBeforeSave_doesNotSave() {
        // Given
        account.setAccountStatus("Blocked");
        when(users.findByIdForUpdate(7)).thenReturn(Optional.of(account));

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.changePassword(snapshot, "NewPass123!", EXPIRES_AT));

        // Then
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        assertEquals("old-hash", account.getPasswordHash());
        verify(encoder, times(0)).encode(anyString());
        verify(users, times(0)).saveAndFlush(any(User.class));
    }
}
