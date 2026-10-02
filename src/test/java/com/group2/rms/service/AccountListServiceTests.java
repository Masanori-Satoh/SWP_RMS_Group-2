package com.group2.rms.service;

import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.service.AccountListService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AccountListServiceTests {

    @Test
    void deactivationPreservesAccountAndPasswordHash() {
        UserRepository users = mock(UserRepository.class);
        User account = User.builder().userId(12).username("employee12")
                .role(Role.builder().roleName("HR").build())
                .accountStatus("Active").passwordHash("existing-bcrypt-hash").build();
        when(users.findById(12)).thenReturn(Optional.of(account));

        new AccountListService(users, mock(RoleRepository.class), mock(DepartmentRepository.class), mock(com.group2.rms.repository.CandidateRepository.class))
                .deactivate(12);

        assertEquals("Inactive", account.getAccountStatus());
        assertEquals("existing-bcrypt-hash", account.getPasswordHash());
        verify(users).findById(12);
        verify(users).save(account);
        verifyNoMoreInteractions(users);
    }

    @Test
    void deactivationRejectsCrossGroupTargetsAndPreservesInactive() {
        UserRepository users = mock(UserRepository.class);
        AccountListService service = new AccountListService(users, mock(RoleRepository.class), mock(DepartmentRepository.class), mock(com.group2.rms.repository.CandidateRepository.class));
        User candidate = User.builder().userId(7).role(Role.builder().roleName("Candidate").build()).accountStatus("Active").build();
        User employee = User.builder().userId(8).role(Role.builder().roleName("HR").build()).accountStatus("Inactive").build();
        when(users.findById(7)).thenReturn(Optional.of(candidate));
        when(users.findById(8)).thenReturn(Optional.of(employee));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.deactivate(7));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.deactivateCandidate(8));
        service.deactivate(8);
        org.mockito.Mockito.verify(users, org.mockito.Mockito.never()).save(employee);
        assertEquals("Active", candidate.getAccountStatus());
        service.deactivateCandidate(7);
        assertEquals("Inactive", candidate.getAccountStatus());
        verify(users).save(candidate);
    }
}
