package com.group2.rms.user.service;

import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.candidate.domain.repository.CandidateRepository;
import com.group2.rms.user.entity.*;
import com.group2.rms.user.exception.AccountFieldException;
import com.group2.rms.user.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountManagementCoverageTests {
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final CandidateRepository candidates = mock(CandidateRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AccountManagementService service = new AccountManagementService(users, roles, departments, candidates, encoder);
    private final Role hr = Role.builder().roleId(2).roleName("HR").build();
    private final Role candidateRole = Role.builder().roleId(6).roleName("Candidate").build();
    private final Department department = Department.builder().departmentId(3).departmentStatus("Active").build();

    @Test
    @DisplayName("ACCOUNT-UNIT-01 | fullName Null: từ chối trước khi lưu")
    void create_fullNameNull_rejectsBeforeSaving() {
        // Given: fullName Null: từ chối trước khi lưu.
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            null, "employee", "employee@example.test", null, 2, 3, "ValidPass123!");

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("fullName", error.getField());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-02 | fullName Empty: từ chối trước khi lưu")
    void create_fullNameEmpty_rejectsBeforeSaving() {
        // Given: fullName Empty: từ chối trước khi lưu.
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            "", "employee", "employee@example.test", null, 2, 3, "ValidPass123!");

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("fullName", error.getField());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-03 | fullName Blank: từ chối trước khi lưu")
    void create_fullNameBlank_rejectsBeforeSaving() {
        // Given: fullName Blank: từ chối trước khi lưu.
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            "  ", "employee", "employee@example.test", null, 2, 3, "ValidPass123!");

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("fullName", error.getField());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-04 | username Null: từ chối trước khi lưu")
    void create_usernameNull_rejectsBeforeSaving() {
        // Given: username Null: từ chối trước khi lưu.
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            "Name", null, "employee@example.test", null, 2, 3, "ValidPass123!");

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("username", error.getField());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-05 | username Empty: từ chối trước khi lưu")
    void create_usernameEmpty_rejectsBeforeSaving() {
        // Given: username Empty: từ chối trước khi lưu.
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            "Name", "", "employee@example.test", null, 2, 3, "ValidPass123!");

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("username", error.getField());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-06 | username Blank: từ chối trước khi lưu")
    void create_usernameBlank_rejectsBeforeSaving() {
        // Given: username Blank: từ chối trước khi lưu.
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            "Name", "  ", "employee@example.test", null, 2, 3, "ValidPass123!");

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("username", error.getField());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-07 | email Null: từ chối trước khi lưu")
    void create_emailNull_rejectsBeforeSaving() {
        // Given: email Null: từ chối trước khi lưu.
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            "Name", "employee", null, null, 2, 3, "ValidPass123!");

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("email", error.getField());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-08 | email Empty: từ chối trước khi lưu")
    void create_emailEmpty_rejectsBeforeSaving() {
        // Given: email Empty: từ chối trước khi lưu.
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            "Name", "employee", "", null, 2, 3, "ValidPass123!");

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("email", error.getField());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-09 | email Blank: từ chối trước khi lưu")
    void create_emailBlank_rejectsBeforeSaving() {
        // Given: email Blank: từ chối trước khi lưu.
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            "Name", "employee", "  ", null, 2, 3, "ValidPass123!");

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("email", error.getField());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-10 | Password Null: không tìm role, mã hóa hoặc lưu")
    void create_passwordNull_rejectsBeforeRoleLookup() {
        // Given: Password Null: không tìm role, mã hóa hoặc lưu.
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            "Name", "employee", "employee@example.test", null, 2, 3, null);

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("password", error.getField());
        verify(roles, times(0)).findById(any());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-11 | Password Empty: không tìm role, mã hóa hoặc lưu")
    void create_passwordEmpty_rejectsBeforeRoleLookup() {
        // Given: Password Empty: không tìm role, mã hóa hoặc lưu.
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            "Name", "employee", "employee@example.test", null, 2, 3, "");

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("password", error.getField());
        verify(roles, times(0)).findById(any());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-12 | Password Blank: không tìm role, mã hóa hoặc lưu")
    void create_passwordBlank_rejectsBeforeRoleLookup() {
        // Given: Password Blank: không tìm role, mã hóa hoặc lưu.
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            "Name", "employee", "employee@example.test", null, 2, 3, " ");

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("password", error.getField());
        verify(roles, times(0)).findById(any());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-13 | Password TooShort: không tìm role, mã hóa hoặc lưu")
    void create_passwordTooShort_rejectsBeforeRoleLookup() {
        // Given: Password TooShort: không tìm role, mã hóa hoặc lưu.
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            "Name", "employee", "employee@example.test", null, 2, 3, "short");

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("password", error.getField());
        verify(roles, times(0)).findById(any());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-14 | Password TooLong: không tìm role, mã hóa hoặc lưu")
    void create_passwordTooLong_rejectsBeforeRoleLookup() {
        // Given: Password TooLong: không tìm role, mã hóa hoặc lưu.
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            "Name", "employee", "employee@example.test", null, 2, 3, "a".repeat(33));

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("password", error.getField());
        verify(roles, times(0)).findById(any());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-15 | Password đúng 8 ký tự: tạo internal account và mã hóa một lần")
    void createInternal_passwordLength8_returnsSavedIdAndEncodesOnce() {
        // Given: Password đúng 8 ký tự: tạo internal account và mã hóa một lần.
        when(roles.findById(2)).thenReturn(Optional.of(hr));
        when(departments.findById(3)).thenReturn(Optional.of(department));
        when(encoder.encode("a".repeat(8))).thenReturn("encoded-hash");
        User saved = User.builder().userId(7).build();
        when(users.saveAndFlush(any(User.class))).thenReturn(saved);
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            " Name ", " employee ", " employee@example.test ", null, 2, 3, "a".repeat(8));

        // When: Thực hiện thao tác cần kiểm tra.
        int result = service.createInternal(input);

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals(7, result);
        verify(encoder, times(1)).encode("a".repeat(8));
        verify(users, times(1)).findByUsernameIgnoreCase("employee");
        verify(users, times(1)).findByEmailIgnoreCase("employee@example.test");
        verify(users, times(1)).saveAndFlush(any(User.class));
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-16 | Password đúng 32 ký tự: tạo internal account và mã hóa một lần")
    void createInternal_passwordLength32_returnsSavedIdAndEncodesOnce() {
        // Given: Password đúng 32 ký tự: tạo internal account và mã hóa một lần.
        when(roles.findById(2)).thenReturn(Optional.of(hr));
        when(departments.findById(3)).thenReturn(Optional.of(department));
        when(encoder.encode("a".repeat(32))).thenReturn("encoded-hash");
        User saved = User.builder().userId(7).build();
        when(users.saveAndFlush(any(User.class))).thenReturn(saved);
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            " Name ", " employee ", " employee@example.test ", null, 2, 3, "a".repeat(32));

        // When: Thực hiện thao tác cần kiểm tra.
        int result = service.createInternal(input);

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals(7, result);
        verify(encoder, times(1)).encode("a".repeat(32));
        verify(users, times(1)).findByUsernameIgnoreCase("employee");
        verify(users, times(1)).findByEmailIgnoreCase("employee@example.test");
        verify(users, times(1)).saveAndFlush(any(User.class));
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-17 | Role Null: trả lỗi roleId và không lưu")
    void create_roleNull_rejectsBeforeSaving() {
        // Given: Role Null: trả lỗi roleId và không lưu.
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            "Name", "employee", "employee@example.test", null, null, 3, "ValidPass123!");

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("roleId", error.getField());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-18 | Role Missing: trả lỗi roleId và không lưu")
    void create_roleMissing_rejectsBeforeSaving() {
        // Given: Role Missing: trả lỗi roleId và không lưu.
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            "Name", "employee", "employee@example.test", null, 77, 3, "ValidPass123!");

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("roleId", error.getField());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-19 | Role Unsupported: trả lỗi roleId và không lưu")
    void create_roleUnsupported_rejectsBeforeSaving() {
        // Given: Role Unsupported: trả lỗi roleId và không lưu.
        when(roles.findById(8)).thenReturn(Optional.of(Role.builder().roleName("Unknown").build()));
        AccountManagementService.CreateCommand input = new AccountManagementService.CreateCommand(
            "Name", "employee", "employee@example.test", null, 8, 3, "ValidPass123!");

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(input));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("roleId", error.getField());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-20 | Trạng thái Null: giữ thông tin cũ và không lưu")
    void update_statusNull_preservesAccount() {
        // Given: Trạng thái Null: giữ thông tin cũ và không lưu.
        User account = User.builder().userId(7).username("employee").email("employee@example.test")
            .fullName("Original").passwordHash("old-hash").role(hr).department(department).accountStatus("Active").build();
        when(users.findById(7)).thenReturn(Optional.of(account));
        when(roles.findById(2)).thenReturn(Optional.of(hr));
        when(departments.findById(3)).thenReturn(Optional.of(department));

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.update(7,
            new AccountManagementService.UpdateCommand("Changed", "changed@example.test", null, 2, 3, null)));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("accountStatus", error.getField());
        assertEquals("Original", account.getFullName());
        assertEquals("employee@example.test", account.getEmail());
        assertEquals("Active", account.getAccountStatus());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-21 | Trạng thái Empty: giữ thông tin cũ và không lưu")
    void update_statusEmpty_preservesAccount() {
        // Given: Trạng thái Empty: giữ thông tin cũ và không lưu.
        User account = User.builder().userId(7).username("employee").email("employee@example.test")
            .fullName("Original").passwordHash("old-hash").role(hr).department(department).accountStatus("Active").build();
        when(users.findById(7)).thenReturn(Optional.of(account));
        when(roles.findById(2)).thenReturn(Optional.of(hr));
        when(departments.findById(3)).thenReturn(Optional.of(department));

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.update(7,
            new AccountManagementService.UpdateCommand("Changed", "changed@example.test", null, 2, 3, "")));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("accountStatus", error.getField());
        assertEquals("Original", account.getFullName());
        assertEquals("employee@example.test", account.getEmail());
        assertEquals("Active", account.getAccountStatus());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-22 | Trạng thái Unsupported: giữ thông tin cũ và không lưu")
    void update_statusUnsupported_preservesAccount() {
        // Given: Trạng thái Unsupported: giữ thông tin cũ và không lưu.
        User account = User.builder().userId(7).username("employee").email("employee@example.test")
            .fullName("Original").passwordHash("old-hash").role(hr).department(department).accountStatus("Active").build();
        when(users.findById(7)).thenReturn(Optional.of(account));
        when(roles.findById(2)).thenReturn(Optional.of(hr));
        when(departments.findById(3)).thenReturn(Optional.of(department));

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.update(7,
            new AccountManagementService.UpdateCommand("Changed", "changed@example.test", null, 2, 3, "Deleted")));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("accountStatus", error.getField());
        assertEquals("Original", account.getFullName());
        assertEquals("employee@example.test", account.getEmail());
        assertEquals("Active", account.getAccountStatus());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-23 | Account chưa có phòng ban: cho gán phòng Active, giữ username và password")
    void updateInternal_missingCurrentDepartment_assignsActiveDepartment() {
        // Given: Account chưa có phòng ban: cho gán phòng Active, giữ username và password.
        User account = User.builder().userId(7).username("employee").email("employee@example.test")
            .fullName("Original").passwordHash("old-hash").role(hr).department(department).accountStatus("Active").build();
        when(users.findById(7)).thenReturn(Optional.of(account));
        account.setDepartment(null);
        when(roles.findById(2)).thenReturn(Optional.of(hr));
        when(departments.findById(3)).thenReturn(Optional.of(department));

        // When: Thực hiện thao tác cần kiểm tra.
        service.updateInternal(7, new AccountManagementService.UpdateCommand("Name", "employee@example.test", null, 2, 3, "Active"));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals(department, account.getDepartment());
        assertEquals("employee", account.getUsername());
        assertEquals("old-hash", account.getPasswordHash());
        verify(users, times(1)).saveAndFlush(account);
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-24 | Đọc account không có phòng ban: trả đúng dữ liệu hiện tại")
    void findForEdit_withoutDepartment_mapsCurrentAccount() {
        // Given: Đọc account không có phòng ban: trả đúng dữ liệu hiện tại.
        User account = User.builder().userId(7).username("employee").email("employee@example.test")
            .fullName("Original").passwordHash("old-hash").role(hr).department(department).accountStatus("Active").build();
        when(users.findById(7)).thenReturn(Optional.of(account));
        account.setDepartment(null);

        // When: Thực hiện thao tác cần kiểm tra.
        AccountManagementService.AccountForEdit result = service.findForEdit(7);

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("employee", result.username());
        assertEquals(null, result.departmentId());
        assertEquals(2, result.roleId());
        verify(users, times(0)).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-25 | Đọc account có phòng ban: trả đúng dữ liệu hiện tại")
    void findForEdit_withDepartment_mapsCurrentAccount() {
        // Given: Đọc account có phòng ban: trả đúng dữ liệu hiện tại.
        User account = User.builder().userId(7).username("employee").email("employee@example.test")
            .fullName("Original").passwordHash("old-hash").role(hr).department(department).accountStatus("Active").build();
        when(users.findById(7)).thenReturn(Optional.of(account));

        // When: Thực hiện thao tác cần kiểm tra.
        AccountManagementService.AccountForEdit result = service.findForEdit(7);

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("employee", result.username());
        assertEquals(Integer.valueOf(3), result.departmentId());
        assertEquals(2, result.roleId());
        verify(users, times(0)).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-26 | Account không tồn tại: trả 404")
    void findForEdit_missingAccount_returnsNotFound() {
        // Given: Account không tồn tại: trả 404.
        // Repository mock chưa có account ID 77.

        // When: Thực hiện thao tác cần kiểm tra.
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.findForEdit(77));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals(404, error.getStatusCode().value());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-27 | Candidate không được đọc qua trang sửa internal account")
    void findForEdit_candidateAccount_returnsNotFound() {
        // Given: Candidate không được đọc qua trang sửa internal account.
        when(users.findById(8)).thenReturn(Optional.of(User.builder().userId(8).role(candidateRole).build()));

        // When: Thực hiện thao tác cần kiểm tra.
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.findForEdit(8));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals(404, error.getStatusCode().value());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-28 | Username và email trống trong DB: trim trước khi kiểm tra trùng")
    void assertLoginIdentifiersAvailable_freeInputs_trimBeforeLookingUp() {
        // Given: Username và email trống trong DB: trim trước khi kiểm tra trùng.
        // Không mock kết quả trùng: repository trả Optional.empty().

        // When: Thực hiện thao tác cần kiểm tra.
        service.assertLoginIdentifiersAvailable(" employee ", " employee@example.test ");

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        verify(users, times(1)).findByUsernameIgnoreCase("employee");
        verify(users, times(1)).findByEmailIgnoreCase("employee");
        verify(users, times(1)).findByEmailIgnoreCase("employee@example.test");
        verify(users, times(1)).findByUsernameIgnoreCase("employee@example.test");
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-29 | username trùng chéo identifier của account khác: báo đúng field")
    void assertLoginIdentifiersAvailable_usernameCrossCollision_rejectsCorrectField() {
        // Given: username trùng chéo identifier của account khác: báo đúng field.
        User other = User.builder().userId(99).build();
        when(users.findByEmailIgnoreCase("employee")).thenReturn(Optional.of(other));

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class,
            () -> service.assertLoginIdentifiersAvailable("employee", "employee@example.test"));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("username", error.getField());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-30 | email trùng chéo identifier của account khác: báo đúng field")
    void assertLoginIdentifiersAvailable_emailCrossCollision_rejectsCorrectField() {
        // Given: email trùng chéo identifier của account khác: báo đúng field.
        User other = User.builder().userId(99).build();
        when(users.findByUsernameIgnoreCase("employee@example.test")).thenReturn(Optional.of(other));

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class,
            () -> service.assertLoginIdentifiersAvailable("employee", "employee@example.test"));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("email", error.getField());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-31 | Đăng ký đã chuẩn bị hash: lưu account/profile, không mã hóa lại")
    void createCandidateWithPasswordHash_usesPreparedHashWithoutEncoding() {
        // Given: Đăng ký đã chuẩn bị hash: lưu account/profile, không mã hóa lại.
        when(roles.findByRoleName("Candidate")).thenReturn(Optional.of(candidateRole));
        when(users.saveAndFlush(any(User.class))).thenReturn(User.builder().userId(7).build());

        // When: Thực hiện thao tác cần kiểm tra.
        int result = service.createCandidateWithPasswordHash("Name", "candidate", "candidate@example.test", "prepared-hash");

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals(7, result);
        verify(users, times(1)).findByUsernameIgnoreCase("candidate");
        verify(users, times(1)).findByEmailIgnoreCase("candidate@example.test");
        verify(users, times(1)).saveAndFlush(any(User.class));
        verify(candidates, times(1)).save(any(Candidate.class));
        verify(encoder, times(0)).encode(any());
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-32 | Chưa cấu hình role Candidate: không được tạo account")
    void createCandidateWithPasswordHash_missingRole_rejectsBeforeSaving() {
        // Given: Chưa cấu hình role Candidate: không được tạo account.
        // roles.findByRoleName trả Optional.empty().

        // When: Thực hiện thao tác cần kiểm tra.
        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> service.createCandidateWithPasswordHash("Name", "candidate", "candidate@example.test", "prepared-hash"));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("Candidate role is not configured", error.getMessage());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-33 | Role nội bộ chọn phòng ban không tồn tại: trả lỗi departmentId")
    void create_missingDepartment_rejectsBeforeSaving() {
        // Given: Role nội bộ chọn phòng ban không tồn tại: trả lỗi departmentId.
        when(roles.findById(2)).thenReturn(Optional.of(hr));

        // When: Thực hiện thao tác cần kiểm tra.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.create(
            new AccountManagementService.CreateCommand("Name", "employee", "employee@example.test", null, 2, 77, "ValidPass123!")));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals("departmentId", error.getField());
        verify(users, times(0)).saveAndFlush(any(User.class));
        verify(encoder, times(0)).encode(any());
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-34 | Lưu account lỗi: giữ exception và không tạo profile dở dang")
    void createCandidate_accountRepositoryFailure_doesNotCreateProfile() {
        // Given: Lưu account lỗi: giữ exception và không tạo profile dở dang.
        when(roles.findByRoleName("Candidate")).thenReturn(Optional.of(candidateRole));
        IllegalStateException failure = new IllegalStateException("write failed");
        when(users.saveAndFlush(any(User.class))).thenThrow(failure);

        // When: Thực hiện thao tác cần kiểm tra.
        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> service.createCandidateWithPasswordHash("Name", "candidate", "candidate@example.test", "prepared-hash"));

        // Then: Kiểm tra kết quả nghiệp vụ và số lần gọi dependency.
        assertEquals(failure, error);
        verify(candidates, times(0)).save(any(Candidate.class));
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-35 | Cập nhật nội bộ đổi thông tin và role nhưng giữ username/hash")
    void updateInternal_validChanges_updatesAllMutableFields() {
        // Given: Account HR đang Active, role mới HM và phòng ban hợp lệ.
        User account = User.builder().userId(7).username("employee").passwordHash("old-hash")
            .fullName("Old Name").phoneNumber("old-phone").role(hr).department(department).accountStatus("Active").build();
        Role hm = Role.builder().roleId(4).roleName("Hiring Manager").build();
        when(users.findById(7)).thenReturn(Optional.of(account));
        when(roles.findById(4)).thenReturn(Optional.of(hm));
        when(departments.findById(3)).thenReturn(Optional.of(department));
        // When: Cập nhật tên, email, phone, role và trạng thái.
        service.updateInternal(7, new AccountManagementService.UpdateCommand(
            "  New Name  ", "  new@example.test  ", "  0901234567  ", 4, 3, "Inactive"));
        // Then: Mọi trường được phép đổi đều đúng; username/hash được giữ, lưu một lần.
        assertEquals("New Name", account.getFullName());
        assertEquals("new@example.test", account.getEmail());
        assertEquals("0901234567", account.getPhoneNumber());
        assertEquals(hm, account.getRole());
        assertEquals(department, account.getDepartment());
        assertEquals("Inactive", account.getAccountStatus());
        assertEquals("employee", account.getUsername());
        assertEquals("old-hash", account.getPasswordHash());
        verify(users, times(1)).saveAndFlush(account);
        verify(encoder, times(0)).encode(any());
    }

    @Test
    @DisplayName("ACCOUNT-UNIT-36 | Form nội bộ không được chọn role Candidate")
    void updateInternal_candidateTarget_rejectsAtInternalRoleGuard() {
        // Given: Account hiện tại là HR nhưng form yêu cầu role Candidate.
        User account = User.builder().userId(7).role(hr).build();
        when(users.findById(7)).thenReturn(Optional.of(account));
        when(roles.findById(1)).thenReturn(Optional.of(candidateRole));
        // When: Cập nhật qua nghiệp vụ dành riêng tài khoản nội bộ.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> service.updateInternal(7,
            new AccountManagementService.UpdateCommand("Name", "a@example.test", null, 1, null, "Active")));
        // Then: Chặn ở guard nội bộ trước khi chạy logic update hoặc lưu dữ liệu.
        assertEquals("roleId", error.getField());
        assertEquals("Select an internal role. Candidate accounts have a separate lifecycle.", error.getMessage());
        verify(users, times(1)).findById(7);
        verify(users, times(0)).saveAndFlush(any(User.class));
    }
}
