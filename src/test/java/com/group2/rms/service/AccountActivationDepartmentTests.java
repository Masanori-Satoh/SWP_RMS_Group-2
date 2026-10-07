package com.group2.rms.service;

import com.group2.rms.candidate.repository.CandidateRepository;
import com.group2.rms.user.entity.*;
import com.group2.rms.user.exception.AccountFieldException;
import com.group2.rms.user.repository.*;
import com.group2.rms.user.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
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

    @Test void restoresInternalAndCandidateWithoutChangingHashOrLifecycle() {
        for(String role:new String[]{"HR","Candidate"}) {
            User u=account(role,"Inactive"); when(users.findById(9)).thenReturn(Optional.of(u));
            if(role.equals("Candidate")) list.activateCandidate(9); else list.activate(9);
            assertEquals("Active",u.getAccountStatus()); assertEquals(role,u.getRole().getRoleName());
            assertEquals("original-hash",u.getPasswordHash()); assertEquals("stable",u.getUsername()); assertSame(inactive,u.getDepartment());
        }
        verifyNoInteractions(candidates,encoder,departments);
        verify(users,never()).deleteById(any());
    }
    @Test void wrongLifecycleMissingAndBlockedCannotBeActivated() {
        User u=account("Candidate","Inactive"); when(users.findById(9)).thenReturn(Optional.of(u));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->list.activate(9)).getStatusCode().value());
        u.setRole(hr); assertThrows(ResponseStatusException.class,()->list.activateCandidate(9));
        u.setAccountStatus("Blocked"); assertEquals(409,assertThrows(ResponseStatusException.class,()->list.activate(9)).getStatusCode().value());
        assertThrows(ResponseStatusException.class,()->list.activate(99)); verify(users,never()).save(any());
    }
    @Test void alreadyActiveIsIdempotent() {
        when(users.findById(9)).thenReturn(Optional.of(account("HR","Active"))); list.activate(9); verify(users,never()).save(any());
    }
    @Test void newAccountCannotJoinInactiveDepartment() {
        when(roles.findById(2)).thenReturn(Optional.of(hr)); when(departments.findById(7)).thenReturn(Optional.of(inactive));
        assertEquals("departmentId",assertThrows(AccountFieldException.class,()->management.createInternal(new AccountManagementService.CreateCommand(
                "Name","new","new@example.test",null,2,7,"password123"))).getField());
        verify(users,never()).saveAndFlush(any()); verifyNoInteractions(encoder);
    }
    @Test void editingExistingMembershipIsAllowedButTransferToInactiveIsRejected() {
        User u=account("HR","Active"); when(users.findById(9)).thenReturn(Optional.of(u)); when(roles.findById(2)).thenReturn(Optional.of(hr));
        when(departments.findById(7)).thenReturn(Optional.of(inactive));
        var command=new AccountManagementService.UpdateCommand("Name","same@example.test",null,2,7,"Active");
        management.updateInternal(9,command); assertSame(inactive,u.getDepartment()); assertEquals("original-hash",u.getPasswordHash());
        clearInvocations(users); u.setDepartment(Department.builder().departmentId(8).build());
        assertThrows(AccountFieldException.class,()->management.updateInternal(9,command)); verify(users,never()).saveAndFlush(any());
    }
    private User account(String role,String status) {
        return User.builder().userId(9).username("stable").fullName("Name").email("same@example.test").passwordHash("original-hash")
                .role(Role.builder().roleId(2).roleName(role).build()).accountStatus(status).department(inactive).build();
    }
}
