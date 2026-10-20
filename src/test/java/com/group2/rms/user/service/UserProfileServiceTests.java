package com.group2.rms.user.service;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.user.dto.*;
import com.group2.rms.user.entity.*;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UserProfileServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UserProfileService service = new UserProfileService(users, encoder);
    private final User user = User.builder().userId(7).username("employee").email("employee@example.test")
            .fullName("Employee").role(Role.builder().roleName("HR").build()).passwordHash("old-hash").build();

    @Test
    @DisplayName("PROFILE-UNIT-01 | Account HR tồn tại nhưng chưa có phòng ban")
    void getProfile_withoutDepartment_returnsNAAndNoCredentials() {
        // Given: Account HR tồn tại nhưng chưa có phòng ban.
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        // When: Đọc thông tin profile của employee.
        UserProfileResponse result = service.getProfile("employee");
        // Then: departmentName=N/A, đúng ID/role và không trả hash hoặc lưu account.
        assertEquals("N/A", result.departmentName());
        assertEquals(7, result.userId());
        assertEquals("employee", result.username());
        assertEquals("HR", result.roleName());
        assertEquals(false, result.toString().contains("old-hash"));
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("PROFILE-UNIT-02 | Account có phòng Engineering")
    void getProfile_withDepartment_returnsCurrentDepartment() {
        // Given: Account có phòng Engineering.
        user.setDepartment(Department.builder().departmentName("Engineering").build());
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        // When: Đọc thông tin profile.
        var result = service.getProfile("employee");
        // Then: Trả đúng tên phòng và email hiện tại.
        assertEquals("Engineering", result.departmentName());
        assertEquals(user.getEmail(), result.email());
    }

    @Test
    @DisplayName("PROFILE-UNIT-03 | Phone/avatar Null, fullName có khoảng trắng đầu/cuối")
    void updateProfile_emptyOptionalValues_storeNullAndPreserveIdentity_Null() {
        // Given: Phone/avatar Null, fullName có khoảng trắng đầu/cuối.
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        // When: Cập nhật profile.
        service.updateProfile("employee", new UpdateProfileRequest("  New Name  ", null, null));
        // Then: Tên được trim, phone/avatar thành null; giữ username/hash, lưu một lần.
        assertEquals("New Name", user.getFullName());
        assertEquals(null, user.getPhoneNumber());
        assertEquals(null, user.getAvatarUrl());
        assertEquals("employee", user.getUsername());
        assertEquals("old-hash", user.getPasswordHash());
        verify(users, times(1)).save(user);
        verify(encoder, times(0)).encode(any());
        verify(encoder, times(0)).matches(any(), any());
    }

    @Test
    @DisplayName("PROFILE-UNIT-04 | Phone/avatar Empty, fullName có khoảng trắng đầu/cuối")
    void updateProfile_emptyOptionalValues_storeNullAndPreserveIdentity_Empty() {
        // Given: Phone/avatar Empty, fullName có khoảng trắng đầu/cuối.
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        // When: Cập nhật profile.
        service.updateProfile("employee", new UpdateProfileRequest("  New Name  ", "", ""));
        // Then: Tên được trim, phone/avatar thành null; giữ username/hash, lưu một lần.
        assertEquals("New Name", user.getFullName());
        assertEquals(null, user.getPhoneNumber());
        assertEquals(null, user.getAvatarUrl());
        assertEquals("employee", user.getUsername());
        assertEquals("old-hash", user.getPasswordHash());
        verify(users, times(1)).save(user);
        verify(encoder, times(0)).encode(any());
        verify(encoder, times(0)).matches(any(), any());
    }

    @Test
    @DisplayName("PROFILE-UNIT-05 | Phone/avatar Blank, fullName có khoảng trắng đầu/cuối")
    void updateProfile_emptyOptionalValues_storeNullAndPreserveIdentity_Blank() {
        // Given: Phone/avatar Blank, fullName có khoảng trắng đầu/cuối.
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        // When: Cập nhật profile.
        service.updateProfile("employee", new UpdateProfileRequest("  New Name  ", "   ", "   "));
        // Then: Tên được trim, phone/avatar thành null; giữ username/hash, lưu một lần.
        assertEquals("New Name", user.getFullName());
        assertEquals(null, user.getPhoneNumber());
        assertEquals(null, user.getAvatarUrl());
        assertEquals("employee", user.getUsername());
        assertEquals("old-hash", user.getPasswordHash());
        verify(users, times(1)).save(user);
        verify(encoder, times(0)).encode(any());
        verify(encoder, times(0)).matches(any(), any());
    }

    @Test
    @DisplayName("PROFILE-UNIT-06 | Tên hợp lệ; phone và avatar có khoảng trắng đầu/cuối")
    void updateProfile_optionalValues_trimBeforeSaving() {
        // Given: Tên hợp lệ; phone và avatar có khoảng trắng đầu/cuối.
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        // When: Cập nhật profile.
        service.updateProfile("employee", new UpdateProfileRequest("New Name", " 0123456789 ", " https://example.test/me.png "));
        // Then: Lưu phone/avatar đã trim đúng một lần.
        assertEquals("0123456789", user.getPhoneNumber());
        assertEquals("https://example.test/me.png", user.getAvatarUrl());
        verify(users, times(1)).save(user);
    }

    @Test
    @DisplayName("PROFILE-UNIT-07 | Account tồn tại nhưng currentPassword không khớp hash cũ")
    void changePassword_wrongCurrentPassword_returnsFalseWithoutEncodingOrSaving() {
        // Given: Account tồn tại nhưng currentPassword không khớp hash cũ.
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        when(encoder.matches("wrong", "old-hash")).thenReturn(false);
        // When: Yêu cầu đổi mật khẩu.
        boolean changed = service.changePassword("employee", new ChangePasswordRequest("wrong", "NewPass123!", "NewPass123!"));
        // Then: Trả false, giữ hash cũ; không encode hoặc save.
        assertEquals(false, changed);
        assertEquals("old-hash", user.getPasswordHash());
        verify(encoder, times(0)).encode(any());
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("PROFILE-UNIT-08 | Current password đúng, mật khẩu mới đã qua validation đầu vào")
    void changePassword_validCurrentPassword_encodesAndSavesOnlyNewHash() {
        // Given: Current password đúng, mật khẩu mới đã qua validation đầu vào.
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        when(encoder.matches("OldPass123!", "old-hash")).thenReturn(true);
        when(encoder.encode("NewPass123!")).thenReturn("new-hash");
        // When: Yêu cầu đổi mật khẩu.
        boolean changed = service.changePassword("employee", new ChangePasswordRequest("OldPass123!", "NewPass123!", "NewPass123!"));
        // Then: Trả true, lưu đúng hash mới một lần, username giữ nguyên.
        assertEquals(true, changed);
        assertEquals("new-hash", user.getPasswordHash());
        assertEquals("employee", user.getUsername());
        verify(encoder, times(1)).encode("NewPass123!");
        verify(users, times(1)).save(user);
    }

    @Test
    @DisplayName("PROFILE-UNIT-09 | Current password đúng nhưng encoder ném lỗi")
    void changePassword_encoderFailure_preservesHashAndDoesNotSave() {
        // Given: Current password đúng nhưng encoder ném lỗi.
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        when(encoder.matches("OldPass123!", "old-hash")).thenReturn(true);
        IllegalArgumentException failure = new IllegalArgumentException("encoding failed");
        when(encoder.encode("NewPass123!")).thenThrow(failure);
        // When: Yêu cầu đổi mật khẩu.
        var error = assertThrows(IllegalArgumentException.class, () -> service.changePassword("employee",
            new ChangePasswordRequest("OldPass123!", "NewPass123!", "NewPass123!")));
        // Then: Giữ nguyên exception và hash cũ; không lưu.
        assertEquals(failure, error);
        assertEquals("old-hash", user.getPasswordHash());
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("PROFILE-UNIT-10 | Repository không tìm thấy account")
    void missingAccount_rejectsProfileReadsUpdatesAndPasswordChanges() {
        // Given: Repository không tìm thấy account.
        // When: Đọc profile, cập nhật profile và đổi password của account không tồn tại.
        // Then: Cả ba thao tác ném ResourceNotFoundException, không save hoặc gọi encoder.
        assertThrows(ResourceNotFoundException.class, () -> service.getProfile("missing"));
        assertThrows(ResourceNotFoundException.class, () -> service.updateProfile("missing", new UpdateProfileRequest("Valid Name", null, null)));
        assertThrows(ResourceNotFoundException.class, () -> service.changePassword("missing", new ChangePasswordRequest("old", "NewPass123!", "NewPass123!")));
        verify(users, times(0)).save(any());
        verify(encoder, times(0)).encode(any());
        verify(encoder, times(0)).matches(any(), any());
    }

}
