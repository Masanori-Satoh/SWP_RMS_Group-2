package com.group2.rms.user.service;

import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class UserAuditLoggerTests {
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final UserRepository users = mock(UserRepository.class);
    final UserAuditLogger audit = new UserAuditLogger(jdbc, users);

    @BeforeEach void session() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "", List.of()));
    }
    @AfterEach void clearSession() { SecurityContextHolder.clearContext(); }

    @Test void registrationRecordsTheNewAccountAsActorAndExcludesCredentials() {
        User registered = User.builder().userId(7).fullName("Ứng viên").username("candidate")
                .passwordHash("secret-password-hash").role(Role.builder().roleName("Candidate").build())
                .accountStatus("Active").build();
        audit.createdUser(registered, true);
        verify(jdbc).update(anyString(), eq(7), eq("CREATE"), eq("User"), eq("7"), isNull(),
                argThat((String value) -> value.startsWith("Đăng ký tài khoản") && !value.contains("secret-password-hash")));
        assertFalse(UserAuditLogger.userSnapshot(registered).toString().contains("password"));
        verifyNoInteractions(users);
    }

    @Test void accountChangesRecordOnlyChangedFieldsWithTheAdminAsActor() {
        when(users.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(User.builder().userId(1).build()));
        audit.changes("User", 7, Map.of("Tên", "Ứng viên", "Trạng thái", "Active"),
                Map.of("Tên", "Ứng viên", "Trạng thái", "Inactive"));
        verify(jdbc).update(anyString(), eq(1), eq("UPDATE"), eq("User"), eq("7"),
                eq("Trạng thái: Active"), eq("Trạng thái: Inactive"));
    }

    @Test void unchangedSaveCreatesNoAuditEntry() {
        audit.changes("Department", 2, Map.of("Tên", "Engineering"), Map.of("Tên", "Engineering"));
        verifyNoInteractions(jdbc, users);
    }

    @Test void departmentCreationIncludesManagerAndStatus() {
        when(users.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(User.builder().userId(1).build()));
        audit.createdDepartment(Department.builder().departmentId(3).departmentName("Engineering").departmentStatus("Active").build());
        verify(jdbc).update(anyString(), eq(1), eq("CREATE"), eq("Department"), eq("3"), isNull(),
                argThat((String value) -> value.contains("Tên phòng ban: Engineering") && value.contains("Trạng thái: Active")));
    }

    @Test void selfDeactivationStillRecordsTheAuthenticatedActor() {
        when(users.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(User.builder().userId(1).accountStatus("Inactive").build()));
        audit.changes("User", 1, Map.of("Trạng thái", "Active"), Map.of("Trạng thái", "Inactive"));
        verify(jdbc).update(anyString(), eq(1), eq("UPDATE"), eq("User"), eq("1"), eq("Trạng thái: Active"), eq("Trạng thái: Inactive"));
    }
}
