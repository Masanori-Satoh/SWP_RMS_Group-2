package com.group2.rms.service;

import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.candidate.repository.CandidateRepository;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.*;
import com.group2.rms.user.service.AccountListService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountInspectorMappingTests {
    final UserRepository users = mock(UserRepository.class);
    final CandidateRepository candidates = mock(CandidateRepository.class);
    final AccountListService service = new AccountListService(users, mock(RoleRepository.class),
            mock(DepartmentRepository.class), candidates);

    @Test void internalInspectorMapsOnlyExistingFieldsWithoutProfileQueriesOrWrites() {
        User employee = User.builder().userId(7).fullName("Nhân viên").username("employee")
                .email("employee@example.test").phoneNumber("0123456789").passwordHash("never-expose")
                .role(Role.builder().roleName("HR").build())
                .department(Department.builder().departmentName("Engineering").build()).accountStatus("Blocked").build();
        employee.setCreatedAt(LocalDateTime.of(2026, 1, 2, 10, 30));
        when(users.findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(employee)));
        var row = service.findAccounts("", null, null, "", "name", 0).getContent().getFirst();
        assertEquals("Engineering", row.departmentName()); assertEquals("Blocked", row.status());
        assertEquals(employee.getPhoneNumber(), row.details().phoneNumber());
        assertEquals(employee.getCreatedAt(), row.details().createdAt());
        assertNull(row.details().updatedAt()); assertNull(row.details().profile());
        assertFalse(row.toString().contains("never-expose"));
        verifyNoInteractions(candidates); verify(users, never()).save(any());
    }

    @Test void candidateInspectorUsesVerifiedBatchRelationAndHandlesMissingProfile() {
        User linked = User.builder().userId(20).fullName("Ứng viên").username("candidate20")
                .role(Role.builder().roleName("Candidate").build()).accountStatus("Active").build();
        User missing = User.builder().userId(21).fullName("Chưa có hồ sơ").username("candidate21")
                .role(Role.builder().roleName("Candidate").build()).accountStatus("Inactive").build();
        Candidate profile = Candidate.builder().candidateId(99).account(linked).dateOfBirth(LocalDate.of(2000, 1, 2))
                .address("Địa chỉ thật từ entity").gender("Other").candidateSource("Website").build();
        profile.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
        when(users.findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(linked, missing)));
        when(candidates.findAllByAccountUserIdIn(List.of(20, 21))).thenReturn(List.of(profile));
        var rows = service.findCandidateAccounts("", "", "name", 0).getContent();
        assertEquals(99, rows.getFirst().profileId()); assertEquals(99, rows.getFirst().details().profile().id());
        assertEquals(profile.getDateOfBirth(), rows.getFirst().details().profile().dateOfBirth());
        assertEquals(profile.getAddress(), rows.getFirst().details().profile().address());
        assertEquals(profile.getCreatedAt(), rows.getFirst().details().profile().createdAt());
        assertNull(rows.get(1).profileId()); assertNull(rows.get(1).details().profile());
        verify(candidates).findAllByAccountUserIdIn(List.of(20, 21)); verifyNoMoreInteractions(candidates);
        verify(users, never()).save(any());
    }

    @Test void emptyCandidatePageDoesNotFetchProfiles() {
        when(users.findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Pageable.class)))
                .thenReturn(Page.empty());
        assertTrue(service.findCandidateAccounts("missing", "", "name", 0).isEmpty());
        verifyNoInteractions(candidates);
    }
}
