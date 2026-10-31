package com.group2.rms.service;

import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.candidate.domain.repository.CandidateRepository;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.*;
import com.group2.rms.user.service.AccountListService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountInspectorMappingTests {
    final UserRepository users = mock(UserRepository.class);
    final CandidateRepository candidates = mock(CandidateRepository.class);
    final AccountListService service = new AccountListService(users, mock(RoleRepository.class),
            mock(DepartmentRepository.class), candidates, org.mockito.Mockito.mock(com.group2.rms.user.service.UserAuditLogger.class));

    @Test
    @DisplayName("Inspector nội bộ map liên hệ/phòng/trạng thái, không lộ hash và không query profile")
    void internalInspectorMapsOnlyExistingFieldsWithoutProfileQueriesOrWrites() {
        // Given: Inspector nội bộ map liên hệ/phòng/trạng thái, không lộ hash và không query profile.
        User employee = User.builder().userId(7).fullName("Nhân viên").username("employee")
            .email("employee@example.test").phoneNumber("0123456789").passwordHash("never-expose")
            .role(Role.builder().roleName("HR").build())
            .department(Department.builder().departmentName("Engineering").build()).accountStatus("Inactive").build();
        employee.setCreatedAt(LocalDateTime.of(2026, 1, 2, 10, 30));
        // Dùng updatedAt khác null để phát hiện việc bỏ sót getter khi map inspector.
        employee.setUpdatedAt(LocalDateTime.of(2026, 1, 3, 11, 45));
        when(users.findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(employee)));
        // When: Gọi service.findAccounts với dữ liệu đã chuẩn bị.
        var row = service.findAccounts("", null, null, "", "name", 0).getContent().getFirst();
        // Then: Inspector nội bộ map liên hệ/phòng/trạng thái, không lộ hash và không query profile.
        assertEquals(7, row.id());
        assertEquals("Nhân viên", row.fullName());
        assertEquals("employee@example.test", row.email());
        assertEquals("employee", row.username());
        assertEquals("HR", row.roleName());
        assertEquals("Engineering", row.departmentName());
        assertEquals("Inactive", row.status());
        assertEquals(employee.getPhoneNumber(), row.details().phoneNumber());
        assertEquals(employee.getCreatedAt(), row.details().createdAt());
        assertEquals(LocalDateTime.of(2026, 1, 3, 11, 45), row.details().updatedAt());
        assertEquals(null, row.details().profile());
        assertEquals(false, row.toString().contains("never-expose"));
        verify(candidates, times(0)).save(any());
        verify(candidates, times(0)).findById(any());
        verify(candidates, times(0)).findByAccountUserId(any());
        verify(candidates, times(0)).findAllByAccountUserIdIn(any());
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("Inspector Candidate map profile theo batch account ID và xử lý profile thiếu")
    void candidateInspectorUsesVerifiedBatchRelationAndHandlesMissingProfile() {
        // Given: Inspector Candidate map profile theo batch account ID và xử lý profile thiếu.
        User linked = User.builder().userId(20).fullName("Ứng viên").username("candidate20")
            .email("candidate20@example.test").phoneNumber("0987654321")
            .role(Role.builder().roleName("Candidate").build()).accountStatus("Active").build();
        // Ngày tạo/cập nhật khác null để assertion không bỏ lọt kết quả map bị mất dữ liệu.
        linked.setCreatedAt(LocalDateTime.of(2026, 2, 1, 8, 30));
        linked.setUpdatedAt(LocalDateTime.of(2026, 2, 2, 9, 45));
        User missing = User.builder().userId(21).fullName("Chưa có hồ sơ").username("candidate21")
            .role(Role.builder().roleName("Candidate").build()).accountStatus("Inactive").build();
        Candidate profile = Candidate.builder().candidateId(99).account(linked).dateOfBirth(LocalDate.of(2000, 1, 2))
            .address("Địa chỉ thật từ entity").gender("Other").candidateSource("Website").build();
        profile.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
        when(users.findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(linked, missing)));
        when(candidates.findAllByAccountUserIdIn(List.of(20, 21))).thenReturn(List.of(profile));
        // When: Gọi service.findCandidateAccounts với dữ liệu đã chuẩn bị.
        var rows = service.findCandidateAccounts("", "", "name", 0).getContent();
        // Then: Inspector Candidate map profile theo batch account ID và xử lý profile thiếu.
        assertEquals(20, rows.getFirst().id());
        assertEquals("Ứng viên", rows.getFirst().fullName());
        assertEquals("candidate20@example.test", rows.getFirst().email());
        assertEquals("candidate20", rows.getFirst().username());
        assertEquals("Active", rows.getFirst().status());
        assertEquals(LocalDateTime.of(2026, 2, 1, 8, 30), rows.getFirst().createdAt());
        assertEquals("0987654321", rows.getFirst().details().phoneNumber());
        assertEquals(LocalDateTime.of(2026, 2, 2, 9, 45), rows.getFirst().details().updatedAt());
        assertEquals(99, rows.getFirst().profileId());
        assertEquals(99, rows.getFirst().details().profile().id());
        assertEquals(profile.getDateOfBirth(), rows.getFirst().details().profile().dateOfBirth());
        assertEquals(profile.getAddress(), rows.getFirst().details().profile().address());
        assertEquals("Other", rows.getFirst().details().profile().gender());
        assertEquals("Website", rows.getFirst().details().profile().source());
        assertEquals(profile.getCreatedAt(), rows.getFirst().details().profile().createdAt());
        assertEquals(null, rows.get(1).profileId());
        assertEquals(21, rows.get(1).id());
        assertEquals("Inactive", rows.get(1).status());
        assertEquals(null, rows.get(1).details().profile());
        verify(candidates, times(1)).findAllByAccountUserIdIn(List.of(20, 21));
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("Trang Candidate rỗng: không query profile")
    void emptyCandidatePageDoesNotFetchProfiles() {
        // Given: Trang Candidate rỗng.
        when(users.findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Pageable.class)))
            .thenReturn(Page.empty());
        // When: Gọi service.findCandidateAccounts với dữ liệu đã chuẩn bị.
        // Then: không query profile.
        assertEquals(true, service.findCandidateAccounts("missing", "", "name", 0).isEmpty());
        verify(candidates, times(0)).save(any());
        verify(candidates, times(0)).findById(any());
        verify(candidates, times(0)).findByAccountUserId(any());
        verify(candidates, times(0)).findAllByAccountUserIdIn(any());
    }
}
