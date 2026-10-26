package com.group2.rms.user.service;

import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.dto.DepartmentRequest;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.exception.DepartmentFieldException;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Regression checks for the role transfer; mocked repositories do not establish database rollback. */
class DepartmentManagerTransferTests {
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final DepartmentService service = new DepartmentService(departments, users, roles);
    private final Role hiringManager = role(3, "Hiring Manager");
    private final Role interviewer = role(5, "Interviewer");
    private final Department department = Department.builder().departmentId(7)
            .departmentName("Engineering").departmentStatus("Active").build();

    @Test
    @DisplayName("MANAGER-TRANSFER-01 | Chuyển trưởng phòng cập nhật role dùng cho lần đăng nhập tiếp theo")
    void replacementChangesLoginAuthoritiesForBothAccounts() {
        User outgoing = member(1, "old-manager", hiringManager);
        User incoming = member(2, "new-manager", interviewer);
        department.setManager(outgoing);
        prepareIncoming(incoming);
        prepareRoles();

        service.update(7, new DepartmentRequest(" Engineering ", incoming.getUserId()));

        assertSame(incoming, department.getManager());
        assertSame(hiringManager, incoming.getRole());
        assertSame(interviewer, outgoing.getRole());
        assertEquals("Engineering", department.getDepartmentName());
        assertSame(department, outgoing.getDepartment());
        assertSame(department, incoming.getDepartment());
        assertEquals("Active", outgoing.getAccountStatus());
        assertEquals("Active", incoming.getAccountStatus());
        verify(departments).findForUpdate(7);
        verify(departments, never()).findById(7);
        verify(departments).saveAndFlush(department);

        when(users.findByUsernameIgnoreCase("old-manager")).thenReturn(Optional.of(outgoing));
        when(users.findByUsernameIgnoreCase("new-manager")).thenReturn(Optional.of(incoming));
        var authentication = new DatabaseUserDetailsService(users);
        assertEquals(List.of("ROLE_INTERVIEWER"), authentication.loadUserByUsername("old-manager")
                .getAuthorities().stream().map(a -> a.getAuthority()).toList());
        assertEquals(List.of("ROLE_HIRING_MANAGER"), authentication.loadUserByUsername("new-manager")
                .getAuthorities().stream().map(a -> a.getAuthority()).toList());
    }

    @Test
    @DisplayName("MANAGER-TRANSFER-02 | Chỉ đổi tên và giữ đúng HM không cần cấu hình role khác")
    void sameManagerWithCorrectRoleKeepsRoleWithoutLookup() {
        User manager = member(1, "manager", hiringManager);
        department.setManager(manager);
        prepareIncoming(manager);

        service.update(7, new DepartmentRequest("Platform", manager.getUserId()));

        assertEquals("Platform", department.getDepartmentName());
        assertSame(manager, department.getManager());
        assertSame(hiringManager, manager.getRole());
        verifyNoInteractions(roles);
        verify(departments).saveAndFlush(department);
    }

    @Test
    @DisplayName("MANAGER-TRANSFER-03 | Giữ cùng manager nhưng role cũ sai được sửa thành HM, không tự hạ role")
    void sameManagerWithMismatchedRoleIsRepairedWithoutDemotion() {
        User manager = member(1, "manager", role(2, "HR"));
        department.setManager(manager);
        prepareIncoming(manager);
        when(roles.findByRoleName("Hiring Manager")).thenReturn(Optional.of(hiringManager));

        service.update(7, new DepartmentRequest("Engineering", manager.getUserId()));

        assertSame(manager, department.getManager());
        assertSame(hiringManager, manager.getRole());
        verify(roles, never()).findByRoleName("Interviewer");
    }

    @Test
    @DisplayName("MANAGER-TRANSFER-04 | Bỏ phân công chuyển trưởng phòng cũ thành Interviewer")
    void clearingManagerDemotesOutgoingAccount() {
        User outgoing = member(1, "old-manager", hiringManager);
        department.setManager(outgoing);
        when(departments.findForUpdate(7)).thenReturn(Optional.of(department));
        when(roles.findByRoleName("Interviewer")).thenReturn(Optional.of(interviewer));

        service.update(7, new DepartmentRequest("Engineering", null));

        assertNull(department.getManager());
        assertSame(interviewer, outgoing.getRole());
        assertSame(department, outgoing.getDepartment());
        assertEquals("Active", outgoing.getAccountStatus());
        verify(roles, never()).findByRoleName("Hiring Manager");
        verify(departments).saveAndFlush(department);
    }

    @Test
    @DisplayName("MANAGER-TRANSFER-05 | Thiếu role HM: giữ nguyên tên, manager và role hai tài khoản")
    void missingIncomingRoleRejectsBeforeAnyMutation() {
        User outgoing = member(1, "old-manager", hiringManager);
        User incoming = member(2, "new-manager", interviewer);
        department.setManager(outgoing);
        prepareIncoming(incoming);
        when(roles.findByRoleName("Hiring Manager")).thenReturn(Optional.empty());
        var request = new DepartmentRequest("Changed", incoming.getUserId());

        var error = assertThrows(DepartmentFieldException.class, () -> service.update(7, request));

        assertFieldErrorAndUnchanged(error, request, outgoing, incoming);
    }

    @Test
    @DisplayName("MANAGER-TRANSFER-06 | Thiếu role Interviewer không promote nửa chừng người mới")
    void missingOutgoingRoleDoesNotPartiallyPromoteIncomingAccount() {
        User outgoing = member(1, "old-manager", hiringManager);
        User incoming = member(2, "new-manager", interviewer);
        department.setManager(outgoing);
        prepareIncoming(incoming);
        when(roles.findByRoleName("Hiring Manager")).thenReturn(Optional.of(hiringManager));
        when(roles.findByRoleName("Interviewer")).thenReturn(Optional.empty());
        var request = new DepartmentRequest("Changed", incoming.getUserId());

        var error = assertThrows(DepartmentFieldException.class, () -> service.update(7, request));

        assertFieldErrorAndUnchanged(error, request, outgoing, incoming);
    }

    @Test
    @DisplayName("MANAGER-TRANSFER-07 | Người mới không Active không được thay manager hoặc role hiện tại")
    void inactiveIncomingManagerRejectsBeforeRoleResolutionOrMutation() {
        User outgoing = member(1, "old-manager", hiringManager);
        User incoming = member(2, "new-manager", interviewer);
        incoming.setAccountStatus("Inactive");
        department.setManager(outgoing);
        prepareIncoming(incoming);
        var request = new DepartmentRequest("Changed", incoming.getUserId());

        var error = assertThrows(DepartmentFieldException.class, () -> service.update(7, request));

        assertFieldErrorAndUnchanged(error, request, outgoing, incoming);
        assertEquals("Inactive", incoming.getAccountStatus());
        verifyNoInteractions(roles);
    }

    @Test
    @DisplayName("MANAGER-TRANSFER-08 | Tên phòng không hợp lệ không được thay role hai người")
    void invalidDepartmentNameDoesNotTransferRoles() {
        User outgoing = member(1, "old-manager", hiringManager);
        User incoming = member(2, "new-manager", interviewer);
        department.setManager(outgoing);
        when(departments.findForUpdate(7)).thenReturn(Optional.of(department));

        var error = assertThrows(DepartmentFieldException.class,
                () -> service.update(7, new DepartmentRequest(" ", incoming.getUserId())));

        assertEquals("departmentName", error.getField());
        assertEquals("Engineering", department.getDepartmentName());
        assertSame(outgoing, department.getManager());
        assertSame(hiringManager, outgoing.getRole());
        assertSame(interviewer, incoming.getRole());
        verifyNoInteractions(roles);
        verify(departments, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("MANAGER-TRANSFER-09 | Phòng chưa phân công chỉ đổi tên không cần role HM hoặc Interviewer")
    void renameUnassignedDepartmentDoesNotResolveRoles() {
        when(departments.findForUpdate(7)).thenReturn(Optional.of(department));

        service.update(7, new DepartmentRequest("Platform", null));

        assertEquals("Platform", department.getDepartmentName());
        assertNull(department.getManager());
        verifyNoInteractions(roles);
        verify(departments).saveAndFlush(department);
    }

    private void assertFieldErrorAndUnchanged(DepartmentFieldException error, DepartmentRequest request,
            User outgoing, User incoming) {
        assertEquals("managerId", error.getField());
        assertEquals(7, error.getDepartmentId());
        assertSame(request, error.getForm());
        assertEquals("Engineering", department.getDepartmentName());
        assertSame(outgoing, department.getManager());
        assertSame(hiringManager, outgoing.getRole());
        assertSame(interviewer, incoming.getRole());
        verify(departments, never()).saveAndFlush(any());
    }

    private void prepareIncoming(User incoming) {
        when(departments.findForUpdate(7)).thenReturn(Optional.of(department));
        when(users.findById(incoming.getUserId())).thenReturn(Optional.of(incoming));
    }

    private void prepareRoles() {
        when(roles.findByRoleName("Hiring Manager")).thenReturn(Optional.of(hiringManager));
        when(roles.findByRoleName("Interviewer")).thenReturn(Optional.of(interviewer));
    }

    private User member(int id, String username, Role role) {
        return User.builder().userId(id).username(username).email(username + "@example.test")
                .fullName(username).passwordHash("unused-test-hash").role(role).department(department)
                .accountStatus("Active").build();
    }

    private Role role(int id, String name) {
        return Role.builder().roleId(id).roleName(name).build();
    }
}
