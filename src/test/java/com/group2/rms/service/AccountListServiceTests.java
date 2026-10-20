package com.group2.rms.service;

import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.candidate.repository.CandidateRepository;
import com.group2.rms.user.service.AccountListService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AccountListServiceTests {

    @Test
    @DisplayName("Vô hiệu hóa account Active: đổi Inactive và giữ password hash")
    void deactivationPreservesAccountAndPasswordHash() {
        // Given: Vô hiệu hóa account Active.
        UserRepository users = mock(UserRepository.class);
        User account = User.builder().userId(12).username("employee12")
            .role(Role.builder().roleName("HR").build())
            .accountStatus("Active").passwordHash("existing-bcrypt-hash").build();
        when(users.findById(12)).thenReturn(Optional.of(account));
        new AccountListService(users, mock(RoleRepository.class), mock(DepartmentRepository.class), mock(CandidateRepository.class))
            .deactivate(12);
        // When: Gọi deactivationPreservesAccountAndPasswordHash với dữ liệu đã chuẩn bị.
        // Then: đổi Inactive và giữ password hash.
        assertEquals("Inactive", account.getAccountStatus());
        assertEquals("existing-bcrypt-hash", account.getPasswordHash());
        verify(users, times(1)).findById(12);
        verify(users, times(1)).save(account);
    }

    @Test
    @DisplayName("Chặn target khác nhóm; giữ account Inactive và cho vô hiệu hóa Candidate đúng nhóm")
    void deactivationRejectsCrossGroupTargetsAndPreservesInactive() {
        // Given: Chặn target khác nhóm; giữ account Inactive và cho vô hiệu hóa Candidate đúng nhóm.
        UserRepository users = mock(UserRepository.class);
        AccountListService service = new AccountListService(users, mock(RoleRepository.class), mock(DepartmentRepository.class), mock(CandidateRepository.class));
        User candidate = User.builder().userId(7).role(Role.builder().roleName("Candidate").build()).accountStatus("Active").build();
        User employee = User.builder().userId(8).role(Role.builder().roleName("HR").build()).accountStatus("Inactive").build();
        when(users.findById(7)).thenReturn(Optional.of(candidate));
        when(users.findById(8)).thenReturn(Optional.of(employee));
        // When: Gọi service.deactivate với dữ liệu đã chuẩn bị.
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.deactivate(7));
        // Then: Chặn target khác nhóm; giữ account Inactive và cho vô hiệu hóa Candidate đúng nhóm.
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.deactivateCandidate(8));
        service.deactivate(8);
        verify(users, times(0)).save(employee);
        assertEquals("Active", candidate.getAccountStatus());
        service.deactivateCandidate(7);
        assertEquals("Inactive", candidate.getAccountStatus());
        verify(users, times(1)).save(candidate);
    }
}
