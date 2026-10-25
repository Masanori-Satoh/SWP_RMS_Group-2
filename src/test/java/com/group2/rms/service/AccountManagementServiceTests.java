package com.group2.rms.service;

import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.candidate.domain.repository.CandidateRepository;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.exception.AccountFieldException;
import com.group2.rms.user.service.AccountManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.*;

class AccountManagementServiceTests {

    @Test
    @DisplayName("Tạo Candidate: mã hóa password, lưu account Active và profile liên kết đúng account")
    void createCandidateHashesPasswordAndLinksSeparateProfile() {
        // Given: Candidate hợp lệ; encoder trả hash và repository trả account đã có ID 77.
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Fixtures fixtures = new Fixtures(encoder);
        Role candidateRole = Role.builder().roleId(6).roleName("Candidate").build();
        User savedAccount = User.builder().userId(77).build();
        User expectedAccount = User.builder().fullName("Ứng viên A").username("candidate77")
            .email("candidate77@example.com").phoneNumber("0900000077").role(candidateRole)
            .passwordHash("encoded-password").accountStatus("Active").build();
        when(fixtures.roles.findById(6)).thenReturn(Optional.of(candidateRole));
        when(encoder.encode("validPassword12")).thenReturn("encoded-password");
        when(fixtures.users.saveAndFlush(any(User.class))).thenReturn(savedAccount);

        // When: Tạo tài khoản Candidate.
        int id = fixtures.service.create(new AccountManagementService.CreateCommand(
            "Ứng viên A", "candidate77", "candidate77@example.com", "0900000077", 6, null, "validPassword12"));

        // Then: Đưa đúng dữ liệu/hash vào repository, tạo profile liên kết account đã lưu và trả đúng ID.
        assertEquals(77, id);
        verify(encoder, times(1)).encode("validPassword12");
        verify(fixtures.users, times(1)).saveAndFlush(refEq(expectedAccount));
        verify(fixtures.candidates, times(1)).save(refEq(Candidate.builder().account(savedAccount).build()));
    }

    @Test
    @DisplayName("Tạo Candidate không nhập phone: lưu phone null và trả đúng ID")
    void candidateWithoutPhoneUsesOptionalUserPhone() {
        // Given: Candidate có phone trống; encoder và repository được mock.
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Fixtures fixtures = new Fixtures(encoder);
        Role candidateRole = Role.builder().roleId(6).roleName("Candidate").build();
        User savedAccount = User.builder().userId(78).build();
        User expectedAccount = User.builder().fullName("Ứng viên B").username("candidate78")
            .email("candidate78@example.com").phoneNumber(null).role(candidateRole)
            .passwordHash("encoded-password").accountStatus("Active").build();
        when(fixtures.roles.findById(6)).thenReturn(Optional.of(candidateRole));
        when(encoder.encode("validPassword12")).thenReturn("encoded-password");
        when(fixtures.users.saveAndFlush(any(User.class))).thenReturn(savedAccount);

        // When: Tạo Candidate không nhập phone.
        int userId = fixtures.service.create(new AccountManagementService.CreateCommand(
            "Ứng viên B", "candidate78", "candidate78@example.com", "", 6, null, "validPassword12"));

        // Then: Phone được chuẩn hóa về null trong account gửi lưu, profile nối đúng account.
        assertEquals(78, userId);
        verify(fixtures.users, times(1)).saveAndFlush(refEq(expectedAccount));
        verify(fixtures.candidates, times(1)).save(refEq(Candidate.builder().account(savedAccount).build()));
        verify(encoder, times(1)).encode("validPassword12");
    }

    @Test
    @DisplayName("Tạo HR thiếu phòng ban hoặc trùng username: báo đúng field và không lưu")
    void createRejectsDuplicateUsernameAndInternalRoleWithoutDepartment() {
        // Given: Tạo HR thiếu phòng ban hoặc trùng username.
        Fixtures fixtures = new Fixtures(mock(PasswordEncoder.class));
        Role hr = Role.builder().roleId(2).roleName("HR").build();
        when(fixtures.roles.findById(2)).thenReturn(Optional.of(hr));
        AccountManagementService.CreateCommand noDepartment =
        new AccountManagementService.CreateCommand(
            "Nhân sự", "hr-new", "hr-new@example.com", null, 2, null,
            "validPassword12");
        assertEquals("departmentId", assertThrows(AccountFieldException.class,
            // When: Gọi fixtures.service.create với dữ liệu đã chuẩn bị.
            () -> fixtures.service.create(noDepartment)).getField());
        Department department = Department.builder().departmentId(1).build();
        when(fixtures.departments.findById(1)).thenReturn(Optional.of(department));
        when(fixtures.users.findByUsernameIgnoreCase("hr-new")).thenReturn(Optional.of(
            User.builder().userId(99).username("hr-new").build()));
        AccountManagementService.CreateCommand duplicate =
        new AccountManagementService.CreateCommand(
            "Nhân sự", "hr-new", "hr-new@example.com", null, 2, 1,
            "validPassword12");
        // Then: báo đúng field và không lưu.
        assertEquals("username", assertThrows(AccountFieldException.class,
            () -> fixtures.service.create(duplicate)).getField());
        verify(fixtures.users, times(0)).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("Cập nhật Candidate: đổi email/trạng thái, giữ username/hash và quan hệ profile")
    void updateKeepsUsernameAndHashAndUsesUserEmailForLinkedCandidate() {
        // Given: Cập nhật Candidate.
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Fixtures fixtures = new Fixtures(encoder);
        Role candidateRole = Role.builder().roleId(6).roleName("Candidate").build();
        User account = User.builder().userId(22).username("stable-login")
            .email("old@example.com").passwordHash("existing-hash")
            .role(candidateRole).accountStatus("Active").build();
        Candidate profile = Candidate.builder().account(account).build();
        when(fixtures.users.findById(22)).thenReturn(Optional.of(account));
        when(fixtures.roles.findById(6)).thenReturn(Optional.of(candidateRole));
        when(fixtures.candidates.findByAccountUserId(22)).thenReturn(Optional.of(profile));
        when(fixtures.users.saveAndFlush(account)).thenReturn(account);
        // When: Gọi fixtures.service.update với dữ liệu đã chuẩn bị.
        fixtures.service.update(22, new AccountManagementService.UpdateCommand(
            "Name mới", "new@example.com", null, 6, null, "Blocked"));
        // Then: đổi email/trạng thái, giữ username/hash và quan hệ profile.
        assertEquals("stable-login", account.getUsername());
        assertEquals("existing-hash", account.getPasswordHash());
        assertEquals("new@example.com", account.getEmail());
        assertEquals("new@example.com", profile.getAccount().getEmail());
        assertEquals("Blocked", account.getAccountStatus());
        verify(fixtures.candidates, times(0)).save(any(Candidate.class));
        verify(encoder, times(0)).encode(any());
        verify(encoder, times(0)).matches(any(), any());
    }

    @Test
    @DisplayName("Cho giữ email chính mình; chặn email account khác và không ghi đè dữ liệu")
    void updateAllowsOwnEmailButRejectsAnotherAccountsEmail() {
        // Given: Cho giữ email chính mình; chặn email account khác và không ghi đè dữ liệu.
        Fixtures fixtures = new Fixtures(mock(PasswordEncoder.class));
        Role hr = Role.builder().roleId(2).roleName("HR").build();
        Department department = Department.builder().departmentId(3).departmentName("HR").build();
        User account = User.builder().userId(10).username("hr10").email("hr10@example.com")
            .passwordHash("hash").role(hr).department(department).accountStatus("Active").build();
        when(fixtures.users.findById(10)).thenReturn(Optional.of(account));
        when(fixtures.roles.findById(2)).thenReturn(Optional.of(hr));
        when(fixtures.departments.findById(3)).thenReturn(Optional.of(department));
        when(fixtures.users.findByEmailIgnoreCase("hr10@example.com")).thenReturn(Optional.of(account));
        when(fixtures.users.saveAndFlush(account)).thenReturn(account);
        // When: Gọi fixtures.service.update với dữ liệu đã chuẩn bị.
        fixtures.service.update(10, new AccountManagementService.UpdateCommand(
            "HR Ten", "hr10@example.com", "", 2, 3, "Active"));
        // Then: Cho giữ email chính mình; chặn email account khác và không ghi đè dữ liệu.
        assertEquals("hash", account.getPasswordHash());
        User another = User.builder().userId(11).email("taken@example.com").build();
        when(fixtures.users.findByEmailIgnoreCase("taken@example.com")).thenReturn(Optional.of(another));
        AccountFieldException error = assertThrows(AccountFieldException.class, () ->
            fixtures.service.update(10, new AccountManagementService.UpdateCommand(
            "HR Ten", "taken@example.com", "", 2, 3, "Active")));
        assertEquals("email", error.getField());
        assertEquals(false, account.getEmail().equals("taken@example.com"));
    }

    @Test
    @DisplayName("Chặn đổi account HR thành Candidate, giữ role/email/hash và không tạo profile")
    void switchingToCandidateIsRejectedWithoutChangingIdentityOrProfile() {
        // Given: Chặn đổi account HR thành Candidate, giữ role/email/hash và không tạo profile.
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Fixtures fixtures = new Fixtures(encoder);
        Role candidateRole = Role.builder().roleId(6).roleName("Candidate").build();
        User account = User.builder().userId(30).username("stable30")
            .email("old30@example.com").passwordHash("original-hash")
            .role(Role.builder().roleId(2).roleName("HR").build())
            .accountStatus("Active").build();
        when(fixtures.users.findById(30)).thenReturn(Optional.of(account));
        when(fixtures.roles.findById(6)).thenReturn(Optional.of(candidateRole));
        // When: Gọi fixtures.service.update với dữ liệu đã chuẩn bị.
        assertThrows(AccountFieldException.class, () -> fixtures.service.update(30, new AccountManagementService.UpdateCommand(
            "Ứng viên chuyển vai trò", "new30@example.com", "0900000030", 6, null, "Active")));
        // Then: Chặn đổi account HR thành Candidate, giữ role/email/hash và không tạo profile.
        assertEquals("HR", account.getRole().getRoleName());
        assertEquals("old30@example.com", account.getEmail());
        assertEquals("original-hash", account.getPasswordHash());
        verify(fixtures.users, times(0)).saveAndFlush(any());
        verify(fixtures.candidates, times(0)).save(any());
        verify(fixtures.candidates, times(0)).findById(any());
        verify(fixtures.candidates, times(0)).findByAccountUserId(any());
        verify(fixtures.candidates, times(0)).findAllByAccountUserIdIn(any());
        verify(encoder, times(0)).encode(any());
        verify(encoder, times(0)).matches(any(), any());
    }

    @Test
    @DisplayName("Tách lifecycle Candidate và nội bộ ở create/update; Candidate không nhận department")
    void candidateCannotBecomeEmployeeAndInternalCreateCannotAcceptCandidate() {
        // Given: Tách lifecycle Candidate và nội bộ ở create/update; Candidate không nhận department.
        Fixtures f = new Fixtures(mock(PasswordEncoder.class));
        Role candidate = Role.builder().roleId(6).roleName("Candidate").build();
        Role hr = Role.builder().roleId(2).roleName("HR").build();
        User account = User.builder().userId(90).username("candidate90").email("original@example.test")
            .role(candidate).accountStatus("Active").passwordHash("unchanged").build();
        when(f.users.findById(90)).thenReturn(Optional.of(account));
        when(f.roles.findById(2)).thenReturn(Optional.of(hr));
        when(f.roles.findById(6)).thenReturn(Optional.of(candidate));
        // When: Gọi f.service.update với dữ liệu đã chuẩn bị.
        assertThrows(AccountFieldException.class, () -> f.service.update(90,
            new AccountManagementService.UpdateCommand("New name", "new@example.test", null, 2, 1, "Active")));
        // Then: Tách lifecycle Candidate và nội bộ ở create/update; Candidate không nhận department.
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> f.service.updateInternal(90,
            new AccountManagementService.UpdateCommand("New name", "new@example.test", null, 2, 1, "Active")));
        assertThrows(AccountFieldException.class, () -> f.service.createInternal(
            new AccountManagementService.CreateCommand("Name", "name", "name@example.test", null, 6, null, "validPassword12")));
        assertEquals(candidate, account.getRole());
        assertEquals("original@example.test", account.getEmail());
        verify(f.users, times(0)).saveAndFlush(any());
        verify(f.candidates, times(0)).save(any());
        verify(f.candidates, times(0)).findById(any());
        verify(f.candidates, times(0)).findByAccountUserId(any());
        verify(f.candidates, times(0)).findAllByAccountUserIdIn(any());
        verify(f.encoder, times(0)).encode(any());
        verify(f.encoder, times(0)).matches(any(), any());
        assertThrows(AccountFieldException.class, () -> f.service.create(
            new AccountManagementService.CreateCommand("Name", "name", "name@example.test", null, 6, 1, "validPassword12")));
    }

    private static class Fixtures {
        final UserRepository users = mock(UserRepository.class);
        final RoleRepository roles = mock(RoleRepository.class);
        final DepartmentRepository departments = mock(DepartmentRepository.class);
        final CandidateRepository candidates = mock(CandidateRepository.class);
        final PasswordEncoder encoder;
        final AccountManagementService service;

        Fixtures(PasswordEncoder encoder) {
            this.encoder = encoder;
            this.service = new AccountManagementService(users, roles, departments, candidates, encoder);
        }
    }
}
