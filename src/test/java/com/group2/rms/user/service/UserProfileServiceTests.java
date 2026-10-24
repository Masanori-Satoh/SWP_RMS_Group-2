package com.group2.rms.user.service;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.user.dto.*;
import com.group2.rms.user.entity.*;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.exception.AvatarUploadException;
import com.group2.rms.user.validator.AvatarFileValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class UserProfileServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private static final String SEED_AVATAR = "https://ui-avatars.com/api/?name=Employee&background=random";
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0x0D};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};

    @TempDir
    Path uploads;

    private LocalAvatarStorage storage;
    private UserProfileService service;
    private final User user = User.builder().userId(7).username("employee").email("employee@example.test")
            .fullName("Employee").role(Role.builder().roleName("HR").build()).passwordHash("old-hash")
            .avatarUrl(SEED_AVATAR).build();

    @BeforeEach
    void setUp() {
        storage = new LocalAvatarStorage(uploads.toString());
        service = new UserProfileService(users, encoder, storage, new AvatarFileValidator());
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("PROFILE-UNIT-01 | Account HR tồn tại nhưng chưa có phòng ban")
    void getProfile_withoutDepartment_returnsNoDepartmentLabelAndNoCredentials() {
        // Given: Account HR tồn tại nhưng chưa có phòng ban.
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        // When: Đọc thông tin profile của employee.
        UserProfileResponse result = service.getProfile("employee");
        // Then: departmentName="Chưa thuộc phòng ban", đúng ID/role (kèm tên tiếng Việt) và không trả hash hoặc lưu account.
        assertEquals("Chưa thuộc phòng ban", result.departmentName());
        assertEquals("Nhân sự", result.roleLabel());
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
    @DisplayName("PROFILE-UNIT-03 | Phone Null, fullName có khoảng trắng đầu/cuối")
    void updateProfile_emptyOptionalValues_storeNullAndPreserveIdentity_Null() {
        // Given: Phone Null, fullName có khoảng trắng đầu/cuối.
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        // When: Cập nhật profile.
        service.updateProfile("employee", new UpdateProfileRequest("  New Name  ", null));
        // Then: Tên được trim, phone thành null, ảnh giữ nguyên; giữ username/hash, lưu một lần.
        assertEquals("New Name", user.getFullName());
        assertEquals(null, user.getPhoneNumber());
        assertEquals(SEED_AVATAR, user.getAvatarUrl());
        assertEquals("employee", user.getUsername());
        assertEquals("old-hash", user.getPasswordHash());
        verify(users, times(1)).save(user);
        verify(encoder, times(0)).encode(any());
        verify(encoder, times(0)).matches(any(), any());
    }

    @Test
    @DisplayName("PROFILE-UNIT-04 | Phone Empty, fullName có khoảng trắng đầu/cuối")
    void updateProfile_emptyOptionalValues_storeNullAndPreserveIdentity_Empty() {
        // Given: Phone Empty, fullName có khoảng trắng đầu/cuối.
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        // When: Cập nhật profile.
        service.updateProfile("employee", new UpdateProfileRequest("  New Name  ", ""));
        // Then: Tên được trim, phone thành null, ảnh giữ nguyên; giữ username/hash, lưu một lần.
        assertEquals("New Name", user.getFullName());
        assertEquals(null, user.getPhoneNumber());
        assertEquals(SEED_AVATAR, user.getAvatarUrl());
        assertEquals("employee", user.getUsername());
        assertEquals("old-hash", user.getPasswordHash());
        verify(users, times(1)).save(user);
        verify(encoder, times(0)).encode(any());
        verify(encoder, times(0)).matches(any(), any());
    }

    @Test
    @DisplayName("PROFILE-UNIT-05 | Phone Blank, fullName có khoảng trắng đầu/cuối")
    void updateProfile_emptyOptionalValues_storeNullAndPreserveIdentity_Blank() {
        // Given: Phone Blank, fullName có khoảng trắng đầu/cuối.
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        // When: Cập nhật profile.
        service.updateProfile("employee", new UpdateProfileRequest("  New Name  ", "   "));
        // Then: Tên được trim, phone thành null, ảnh giữ nguyên; giữ username/hash, lưu một lần.
        assertEquals("New Name", user.getFullName());
        assertEquals(null, user.getPhoneNumber());
        assertEquals(SEED_AVATAR, user.getAvatarUrl());
        assertEquals("employee", user.getUsername());
        assertEquals("old-hash", user.getPasswordHash());
        verify(users, times(1)).save(user);
        verify(encoder, times(0)).encode(any());
        verify(encoder, times(0)).matches(any(), any());
    }

    @Test
    @DisplayName("PROFILE-UNIT-06 | Tên hợp lệ; phone có khoảng trắng đầu/cuối")
    void updateProfile_optionalValues_trimBeforeSaving() {
        // Given: Tên hợp lệ; phone có khoảng trắng đầu/cuối.
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        // When: Cập nhật profile.
        service.updateProfile("employee", new UpdateProfileRequest("New Name", " 0123456789 "));
        // Then: Lưu phone đã trim đúng một lần; ảnh không đổi qua form thông tin.
        assertEquals("0123456789", user.getPhoneNumber());
        assertEquals(SEED_AVATAR, user.getAvatarUrl());
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
        assertThrows(ResourceNotFoundException.class, () -> service.updateProfile("missing", new UpdateProfileRequest("Valid Name", null)));
        assertThrows(ResourceNotFoundException.class, () -> service.changePassword("missing", new ChangePasswordRequest("old", "NewPass123!", "NewPass123!")));
        verify(users, times(0)).save(any());
        verify(encoder, times(0)).encode(any());
        verify(encoder, times(0)).matches(any(), any());
    }

    @Test
    @DisplayName("PROFILE-UNIT-11 | Link ui-avatars cũ, NULL, hoặc file không có trên máy: coi như chưa có ảnh")
    void avatarSrc_nonLocalOrMissingFile_returnsNull() {
        assertNull(service.avatarSrc(user));
        user.setAvatarUrl(null);
        assertNull(service.avatarSrc(user));
        user.setAvatarUrl("local:avatar/7/0b9c2f1e-0000-0000-0000-000000000000.png");
        assertNull(service.avatarSrc(user));
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        assertNull(service.getProfile("employee").avatarUrl());
    }

    @Test
    @DisplayName("PROFILE-UNIT-12 | Tải PNG rồi JPEG, commit: DB ghi local:, link có v= theo file, file cũ bị xóa")
    void changeAvatar_commit_storesLocalValueAndDeletesOldFile() throws Exception {
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        TransactionSynchronizationManager.initSynchronization();
        service.changeAvatar("employee", new MockMultipartFile("avatarFile", "first.png", "image/png", PNG));
        complete(TransactionSynchronization.STATUS_COMMITTED);
        String firstKey = LocalAvatarStorage.keyOf(user.getAvatarUrl());
        assertTrue(user.getAvatarUrl().matches("local:avatar/7/[0-9a-f-]{36}\\.png"), user.getAvatarUrl());
        assertTrue(storage.exists(firstKey));
        String uuid = firstKey.substring(firstKey.lastIndexOf('/') + 1, firstKey.lastIndexOf('.'));
        assertEquals("/users/7/avatar?v=" + uuid, service.avatarSrc(user));

        TransactionSynchronizationManager.initSynchronization();
        service.changeAvatar("employee", new MockMultipartFile("avatarFile", "second.jpg", "image/jpeg", JPEG));
        complete(TransactionSynchronization.STATUS_COMMITTED);
        String secondKey = LocalAvatarStorage.keyOf(user.getAvatarUrl());
        assertTrue(secondKey.endsWith(".jpg"));
        assertFalse(storage.exists(firstKey));
        assertTrue(storage.exists(secondKey));
        try (var files = Files.list(uploads.resolve("avatar/7"))) {
            assertEquals(1, files.count());
        }
        verify(users, times(2)).save(user);
    }

    @Test
    @DisplayName("PROFILE-UNIT-13 | Transaction không commit: xóa file mới, file cũ giữ nguyên")
    void changeAvatar_rollback_deletesNewFileKeepsOld() {
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        String oldKey = storage.store(new MockMultipartFile("avatarFile", "old.png", "image/png", PNG), 7, "png");
        user.setAvatarUrl(LocalAvatarStorage.URL_PREFIX + oldKey);

        TransactionSynchronizationManager.initSynchronization();
        service.changeAvatar("employee", new MockMultipartFile("avatarFile", "new.png", "image/png", PNG));
        String newKey = LocalAvatarStorage.keyOf(user.getAvatarUrl());
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        assertFalse(storage.exists(newKey));
        assertTrue(storage.exists(oldKey));
    }

    @Test
    @DisplayName("PROFILE-UNIT-14 | File SVG: báo lỗi, không lưu file, không đổi DB")
    void changeAvatar_invalidFile_rejectsWithoutStoringOrSaving() {
        when(users.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(user));
        TransactionSynchronizationManager.initSynchronization();
        var svg = new MockMultipartFile("avatarFile", "x.svg", "image/svg+xml", "<svg/>".getBytes());
        assertThrows(AvatarUploadException.class, () -> service.changeAvatar("employee", svg));
        assertEquals(SEED_AVATAR, user.getAvatarUrl());
        assertFalse(Files.exists(uploads.resolve("avatar")));
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("PROFILE-UNIT-15 | /users/{id}/avatar: có file thì trả đúng loại; không ảnh / không có người thì 404")
    void avatarFile_returnsStoredImageOrNotFound() {
        String key = storage.store(new MockMultipartFile("avatarFile", "a.webp", "image/webp", PNG), 7, "webp");
        user.setAvatarUrl(LocalAvatarStorage.URL_PREFIX + key);
        when(users.findById(7)).thenReturn(Optional.of(user));
        assertEquals("image/webp", service.avatarFile(7).contentType());

        user.setAvatarUrl(SEED_AVATAR);
        assertThrows(ResourceNotFoundException.class, () -> service.avatarFile(7));
        when(users.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.avatarFile(99));
    }

    /** Giả lập lúc transaction kết thúc: chạy afterCompletion của các synchronization đã đăng ký. */
    private static void complete(int status) {
        var synchronizations = TransactionSynchronizationManager.getSynchronizations();
        TransactionSynchronizationManager.clearSynchronization();
        synchronizations.forEach(s -> s.afterCompletion(status));
    }
}
