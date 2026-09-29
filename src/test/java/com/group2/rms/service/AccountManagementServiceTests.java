package com.group2.rms.service;

import com.group2.rms.entity.Candidate;
import com.group2.rms.entity.Department;
import com.group2.rms.entity.Role;
import com.group2.rms.entity.User;
import com.group2.rms.repository.CandidateRepository;
import com.group2.rms.repository.DepartmentRepository;
import com.group2.rms.repository.RoleRepository;
import com.group2.rms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccountManagementServiceTests {

    @Test
    void createCandidateHashesPasswordAndLinksSeparateProfile() {
        Fixtures fixtures = new Fixtures(new BCryptPasswordEncoder());
        Role candidateRole = Role.builder().roleId(6).roleName("Candidate").build();
        when(fixtures.roles.findById(6)).thenReturn(Optional.of(candidateRole));
        when(fixtures.users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setUserId(77);
            return saved;
        });

        int id = fixtures.service.create(new AccountManagementService.CreateCommand(
                "Ứng viên A", "candidate77", "candidate77@example.com", "0900000077", 6, null,
                "validPassword12"));

        assertEquals(77, id);
        ArgumentCaptor<User> account = ArgumentCaptor.forClass(User.class);
        verify(fixtures.users).saveAndFlush(account.capture());
        assertEquals("Active", account.getValue().getAccountStatus());
        assertNotEquals("validPassword12", account.getValue().getPasswordHash());
        assertTrue(fixtures.encoder.matches("validPassword12", account.getValue().getPasswordHash()));
        ArgumentCaptor<Candidate> profile = ArgumentCaptor.forClass(Candidate.class);
        verify(fixtures.candidates).save(profile.capture());
        assertEquals(account.getValue(), profile.getValue().getAccount());
        assertEquals("0900000077", account.getValue().getPhoneNumber());
    }

    @Test
    void candidateWithoutPhoneUsesOptionalUserPhone() {
        Fixtures fixtures = new Fixtures(new BCryptPasswordEncoder());
        when(fixtures.roles.findById(6)).thenReturn(Optional.of(
                Role.builder().roleId(6).roleName("Candidate").build()));
        when(fixtures.users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setUserId(78);
            return saved;
        });

        int userId = fixtures.service.create(new AccountManagementService.CreateCommand(
                "Ứng viên B", "candidate78", "candidate78@example.com", "", 6, null,
                "validPassword12"));

        assertEquals(78, userId);
        ArgumentCaptor<Candidate> profile = ArgumentCaptor.forClass(Candidate.class);
        verify(fixtures.candidates).save(profile.capture());
        assertNull(profile.getValue().getAccount().getPhoneNumber());
    }

    @Test
    void createRejectsDuplicateUsernameAndInternalRoleWithoutDepartment() {
        Fixtures fixtures = new Fixtures(mock(PasswordEncoder.class));
        Role hr = Role.builder().roleId(2).roleName("HR").build();
        when(fixtures.roles.findById(2)).thenReturn(Optional.of(hr));
        AccountManagementService.CreateCommand noDepartment =
                new AccountManagementService.CreateCommand(
                        "Nhân sự", "hr-new", "hr-new@example.com", null, 2, null,
                        "validPassword12");
        assertEquals("departmentId", assertThrows(AccountFieldException.class,
                () -> fixtures.service.create(noDepartment)).getField());

        Department department = Department.builder().departmentId(1).build();
        when(fixtures.departments.findById(1)).thenReturn(Optional.of(department));
        when(fixtures.users.findByUsernameIgnoreCase("hr-new")).thenReturn(Optional.of(
                User.builder().userId(99).username("hr-new").build()));
        AccountManagementService.CreateCommand duplicate =
                new AccountManagementService.CreateCommand(
                        "Nhân sự", "hr-new", "hr-new@example.com", null, 2, 1,
                        "validPassword12");
        assertEquals("username", assertThrows(AccountFieldException.class,
                () -> fixtures.service.create(duplicate)).getField());
        verify(fixtures.users, never()).saveAndFlush(any(User.class));
    }

    @Test
    void updateKeepsUsernameAndHashAndUsesUserEmailForLinkedCandidate() {
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

        fixtures.service.update(22, new AccountManagementService.UpdateCommand(
                "Tên mới", "new@example.com", null, 6, null, "Blocked"));

        assertEquals("stable-login", account.getUsername());
        assertEquals("existing-hash", account.getPasswordHash());
        assertEquals("new@example.com", account.getEmail());
        assertEquals("new@example.com", profile.getAccount().getEmail());
        assertEquals("Blocked", account.getAccountStatus());
        verify(fixtures.candidates, never()).save(any(Candidate.class));
        verifyNoInteractions(encoder);
    }

    @Test
    void updateAllowsOwnEmailButRejectsAnotherAccountsEmail() {
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

        fixtures.service.update(10, new AccountManagementService.UpdateCommand(
                "HR Ten", "hr10@example.com", "", 2, 3, "Active"));
        assertEquals("hash", account.getPasswordHash());

        User another = User.builder().userId(11).email("taken@example.com").build();
        when(fixtures.users.findByEmailIgnoreCase("taken@example.com")).thenReturn(Optional.of(another));
        AccountFieldException error = assertThrows(AccountFieldException.class, () ->
                fixtures.service.update(10, new AccountManagementService.UpdateCommand(
                        "HR Ten", "taken@example.com", "", 2, 3, "Active")));
        assertEquals("email", error.getField());
        assertFalse(account.getEmail().equals("taken@example.com"));
    }

    @Test
    void switchingToCandidateCreatesProfileWithoutChangingPassword() {
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Fixtures fixtures = new Fixtures(encoder);
        Role candidateRole = Role.builder().roleId(6).roleName("Candidate").build();
        User account = User.builder().userId(30).username("stable30")
                .email("old30@example.com").passwordHash("original-hash")
                .role(Role.builder().roleId(2).roleName("HR").build())
                .accountStatus("Active").build();
        when(fixtures.users.findById(30)).thenReturn(Optional.of(account));
        when(fixtures.roles.findById(6)).thenReturn(Optional.of(candidateRole));
        when(fixtures.users.saveAndFlush(account)).thenReturn(account);

        fixtures.service.update(30, new AccountManagementService.UpdateCommand(
                "Ứng viên chuyển vai trò", "new30@example.com", "0900000030", 6, null, "Active"));

        ArgumentCaptor<Candidate> profile = ArgumentCaptor.forClass(Candidate.class);
        verify(fixtures.candidates).save(profile.capture());
        assertEquals(account, profile.getValue().getAccount());
        assertEquals("new30@example.com", profile.getValue().getAccount().getEmail());
        assertEquals("original-hash", account.getPasswordHash());
        verifyNoInteractions(encoder);
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
