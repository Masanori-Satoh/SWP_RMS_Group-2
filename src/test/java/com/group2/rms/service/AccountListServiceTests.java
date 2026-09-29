package com.group2.rms.service;

import com.group2.rms.entity.Role;
import com.group2.rms.entity.User;
import com.group2.rms.repository.DepartmentRepository;
import com.group2.rms.repository.RoleRepository;
import com.group2.rms.repository.UserRepository;
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
        User account = User.builder().userId(12).username("candidate12")
                .role(Role.builder().roleName("Candidate").build())
                .accountStatus("Active").passwordHash("existing-bcrypt-hash").build();
        when(users.findById(12)).thenReturn(Optional.of(account));

        new AccountListService(users, mock(RoleRepository.class), mock(DepartmentRepository.class))
                .deactivate(12);

        assertEquals("Inactive", account.getAccountStatus());
        assertEquals("existing-bcrypt-hash", account.getPasswordHash());
        verify(users).findById(12);
        verify(users).save(account);
        verifyNoMoreInteractions(users);
    }
}
