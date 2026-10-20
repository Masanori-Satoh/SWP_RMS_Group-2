package com.group2.rms.service;

import com.group2.rms.candidate.repository.CandidateRepository;
import com.group2.rms.user.entity.*;
import com.group2.rms.user.exception.AccountFieldException;
import com.group2.rms.user.repository.*;
import com.group2.rms.user.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

 
class AccountActivationDepartmentTests {
    private final UserRepository users=mock(UserRepository.class);
    private final RoleRepository roles=mock(RoleRepository.class);
    private final DepartmentRepository departments=mock(DepartmentRepository.class);
    private final CandidateRepository candidates=mock(CandidateRepository.class);
    private final PasswordEncoder encoder=mock(PasswordEncoder.class);
    private final AccountListService list=new AccountListService(users,roles,departments,candidates);
    private final AccountManagementService management=new AccountManagementService(users,roles,departments,candidates,encoder);
    private final Role hr=Role.builder().roleId(2).roleName("HR").build();
    private final Department inactive=Department.builder().departmentId(7).departmentName("Old").departmentStatus("Inactive").build();

    @Test
    @DisplayName("Kích hoạt HR/Candidate Inactive: giữ role, username, hash và membership")
    void restoresInternalAndCandidateWithoutChangingHashOrLifecycle() {
        // Given: Kích hoạt HR/Candidate Inactive.
        for(String role:new String[]{"HR","Candidate"}) {
            User u=account(role,"Inactive");
            when(users.findById(9)).thenReturn(Optional.of(u));
            // When: Gọi list.activateCandidate với dữ liệu đã chuẩn bị.
            if(role.equals("Candidate")) list.activateCandidate(9);
            else list.activate(9);
            // Then: giữ role, username, hash và membership.
            assertEquals("Active",u.getAccountStatus());
            assertEquals(role,u.getRole().getRoleName());
            assertEquals("original-hash",u.getPasswordHash());
            assertEquals("stable",u.getUsername());
            assertEquals(inactive,u.getDepartment());
            // Mỗi account được kích hoạt phải được chuyển cho repository lưu đúng một lần.
            verify(users,times(1)).save(u);
        }
        verify(candidates, times(0)).save(any());
        verify(candidates, times(0)).findById(any());
        verify(candidates, times(0)).findByAccountUserId(any());
        verify(candidates, times(0)).findAllByAccountUserIdIn(any());
        verify(encoder, times(0)).encode(any());
        verify(encoder, times(0)).matches(any(), any());
        verify(departments, times(0)).findById(any());
        verify(departments, times(0)).saveAndFlush(any());
        verify(users,times(0)).deleteById(any());
    }

    @Test
    @DisplayName("Kích hoạt sai nhóm, thiếu account hoặc Blocked: trả 404/409 và không lưu")
    void wrongLifecycleMissingAndBlockedCannotBeActivated() {
        // Given: Kích hoạt sai nhóm, thiếu account hoặc Blocked.
        User u=account("Candidate","Inactive");
        when(users.findById(9)).thenReturn(Optional.of(u));
        // When: Gọi list.activate với dữ liệu đã chuẩn bị.
        // Then: trả 404/409 và không lưu.
        assertEquals(404,assertThrows(ResponseStatusException.class,()->list.activate(9)).getStatusCode().value());
        u.setRole(hr);
        assertThrows(ResponseStatusException.class,()->list.activateCandidate(9));
        u.setAccountStatus("Blocked");
        assertEquals(409,assertThrows(ResponseStatusException.class,()->list.activate(9)).getStatusCode().value());
        assertThrows(ResponseStatusException.class,()->list.activate(99));
        verify(users,times(0)).save(any());
    }

    @Test
    @DisplayName("Kích hoạt account đã Active: không lưu lần nữa")
    void alreadyActiveIsIdempotent() {
        // Given: Kích hoạt account đã Active.
        when(users.findById(9)).thenReturn(Optional.of(account("HR","Active")));
        // When: Gọi list.activate với dữ liệu đã chuẩn bị.
        list.activate(9);
        // Then: không lưu lần nữa.
        verify(users,times(0)).save(any());
    }

    @Test
    @DisplayName("Tạo account nội bộ không được chọn phòng Inactive")
    void newAccountCannotJoinInactiveDepartment() {
        // Given: Tạo account nội bộ không được chọn phòng Inactive.
        when(roles.findById(2)).thenReturn(Optional.of(hr));
        when(departments.findById(7)).thenReturn(Optional.of(inactive));
        // When: Gọi management.createInternal với dữ liệu đã chuẩn bị.
        // Then: Tạo account nội bộ không được chọn phòng Inactive.
        assertEquals("departmentId",assertThrows(AccountFieldException.class,()->management.createInternal(new AccountManagementService.CreateCommand(
            "Name","new","new@example.test",null,2,7,"password123"))).getField());
        verify(users,times(0)).saveAndFlush(any());
        verify(encoder, times(0)).encode(any());
        verify(encoder, times(0)).matches(any(), any());
    }

    @Test
    @DisplayName("Cập nhật account được giữ phòng Inactive đã thuộc từ trước")
    void editingExistingMembershipIsAllowedButTransferToInactiveIsRejected() {
        // Given: Account HR đã thuộc phòng 7 Inactive từ trước.
        User user = account("HR", "Active");
        when(users.findById(9)).thenReturn(Optional.of(user));
        when(roles.findById(2)).thenReturn(Optional.of(hr));
        when(departments.findById(7)).thenReturn(Optional.of(inactive));

        // When: Cập nhật account nhưng giữ nguyên phòng cũ.
        management.updateInternal(9, new AccountManagementService.UpdateCommand(
            "Name", "same@example.test", null, 2, 7, "Active"));

        // Then: Được giữ membership cũ, giữ hash và lưu đúng một lần.
        assertEquals(inactive, user.getDepartment());
        assertEquals("original-hash", user.getPasswordHash());
        verify(users, times(1)).saveAndFlush(user);
    }

    @Test
    @DisplayName("ACCOUNT-LIFECYCLE | Không chuyển account sang phòng Inactive")
    void transferToInactiveDepartment_rejectsWithoutSaving() {
        // Given: Account hiện thuộc phòng 8, yêu cầu chuyển sang phòng 7 Inactive.
        User user = account("HR", "Active");
        user.setDepartment(Department.builder().departmentId(8).build());
        when(users.findById(9)).thenReturn(Optional.of(user));
        when(roles.findById(2)).thenReturn(Optional.of(hr));
        when(departments.findById(7)).thenReturn(Optional.of(inactive));

        // When: Cập nhật phòng ban qua form account nội bộ.
        AccountFieldException error = assertThrows(AccountFieldException.class, () -> management.updateInternal(9,
            new AccountManagementService.UpdateCommand("Name", "same@example.test", null, 2, 7, "Active")));

        // Then: Báo lỗi phòng ban, giữ phòng 8 và không lưu.
        assertEquals("departmentId", error.getField());
        assertEquals(8, user.getDepartment().getDepartmentId());
        verify(users, times(0)).saveAndFlush(any());
    }

    private User account(String role,String status) {
        return User.builder().userId(9).username("stable").fullName("Name").email("same@example.test").passwordHash("original-hash")
                .role(Role.builder().roleId(2).roleName(role).build()).accountStatus(status).department(inactive).build();
    }
}
