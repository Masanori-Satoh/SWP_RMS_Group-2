package com.group2.rms.user.service;

import com.group2.rms.candidate.domain.repository.CandidateRepository;
import com.group2.rms.user.entity.*;
import com.group2.rms.user.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountListCoverageTests {
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final CandidateRepository candidates = mock(CandidateRepository.class);
    private final AccountListService service = new AccountListService(users, roles, departments, candidates, org.mockito.Mockito.mock(com.group2.rms.user.service.UserAuditLogger.class));

    @Test
    @DisplayName("ACCOUNT-LIST-01 | Sort Null: dùng thứ tự và phân trang ổn định")
    void findAccounts_sortNull_usesExpectedOrderAndPage() {
        // Given: Sort Null: dùng thứ tự và phân trang ổn định.
        when(users.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        // When: Gọi service với dữ liệu đã chuẩn bị.
        Page<AccountListService.AccountRow> result = service.findAccounts(null, null, null, "Invalid", null, 2);

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(true, result.isEmpty());
        verify(users, times(1)).findAll(any(Specification.class), eq(PageRequest.of(2, 10, Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("userId")))));
        verify(users, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("ACCOUNT-LIST-02 | Sort Empty: dùng thứ tự và phân trang ổn định")
    void findAccounts_sortEmpty_usesExpectedOrderAndPage() {
        // Given: Sort Empty: dùng thứ tự và phân trang ổn định.
        when(users.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        // When: Gọi service với dữ liệu đã chuẩn bị.
        Page<AccountListService.AccountRow> result = service.findAccounts(null, null, null, "Invalid", "", 2);

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(true, result.isEmpty());
        verify(users, times(1)).findAll(any(Specification.class), eq(PageRequest.of(2, 10, Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("userId")))));
        verify(users, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("ACCOUNT-LIST-03 | Sort NameDesc: dùng thứ tự và phân trang ổn định")
    void findAccounts_sortNameDesc_usesExpectedOrderAndPage() {
        // Given: Sort NameDesc: dùng thứ tự và phân trang ổn định.
        when(users.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        // When: Gọi service với dữ liệu đã chuẩn bị.
        Page<AccountListService.AccountRow> result = service.findAccounts(null, null, null, "Invalid", "name_desc", 2);

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(true, result.isEmpty());
        verify(users, times(1)).findAll(any(Specification.class), eq(PageRequest.of(2, 10, Sort.by(Sort.Order.desc("fullName"), Sort.Order.asc("userId")))));
        verify(users, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("ACCOUNT-LIST-04 | Sort Username: dùng thứ tự và phân trang ổn định")
    void findAccounts_sortUsername_usesExpectedOrderAndPage() {
        // Given: Sort Username: dùng thứ tự và phân trang ổn định.
        when(users.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        // When: Gọi service với dữ liệu đã chuẩn bị.
        Page<AccountListService.AccountRow> result = service.findAccounts(null, null, null, "Invalid", "username", 2);

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(true, result.isEmpty());
        verify(users, times(1)).findAll(any(Specification.class), eq(PageRequest.of(2, 10, Sort.by(Sort.Order.asc("username"), Sort.Order.asc("userId")))));
        verify(users, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("ACCOUNT-LIST-05 | Sort Newest: dùng thứ tự và phân trang ổn định")
    void findAccounts_sortNewest_usesExpectedOrderAndPage() {
        // Given: Sort Newest: dùng thứ tự và phân trang ổn định.
        when(users.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        // When: Gọi service với dữ liệu đã chuẩn bị.
        Page<AccountListService.AccountRow> result = service.findAccounts(null, null, null, "Invalid", "newest", 2);

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(true, result.isEmpty());
        verify(users, times(1)).findAll(any(Specification.class), eq(PageRequest.of(2, 10, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("userId")))));
        verify(users, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("ACCOUNT-LIST-06 | Sort Unknown: dùng thứ tự và phân trang ổn định")
    void findAccounts_sortUnknown_usesExpectedOrderAndPage() {
        // Given: Sort Unknown: dùng thứ tự và phân trang ổn định.
        when(users.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        // When: Gọi service với dữ liệu đã chuẩn bị.
        Page<AccountListService.AccountRow> result = service.findAccounts(null, null, null, "Invalid", "unknown", 2);

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(true, result.isEmpty());
        verify(users, times(1)).findAll(any(Specification.class), eq(PageRequest.of(2, 10, Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("userId")))));
        verify(users, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("ACCOUNT-LIST-07 | Page âm và account chưa có phòng ban: page về 0, departmentName null")
    void findAccounts_negativePageAndMissingDepartment_returnsMappedAccount() {
        // Given: Page âm và account chưa có phòng ban: page về 0, departmentName null.
        User user = User.builder().userId(7).fullName("HR").role(Role.builder().roleName("HR").build()).build();
        when(users.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));

        // When: Gọi service với dữ liệu đã chuẩn bị.
        Page<AccountListService.AccountRow> result = service.findAccounts("HR", 2, 3, "Active", "newest", -1);

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(7, result.getContent().getFirst().id());
        assertEquals(null, result.getContent().getFirst().departmentName());
        verify(users, times(1)).findAll(any(Specification.class), eq(PageRequest.of(0, 10,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("userId")))));
        verify(users, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("ACCOUNT-LIST-08 | Danh sách role nội bộ không chứa Candidate")
    void findRoles_removesCandidateFromInternalChoices() {
        // Given: Danh sách role nội bộ không chứa Candidate.
        Role hr = Role.builder().roleName("HR").build();
        when(roles.findAll(Sort.by("roleName"))).thenReturn(List.of(hr, Role.builder().roleName("Candidate").build()));

        // When: Gọi service với dữ liệu đã chuẩn bị.
        List<Role> result = service.findRoles();

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(List.of(hr), result);
        verify(roles, times(1)).findAll(Sort.by("roleName"));
    }

    @Test
    @DisplayName("ACCOUNT-LIST-09 | Danh sách phòng ban lấy đúng thứ tự theo tên")
    void findDepartments_returnsNameSortedChoices() {
        // Given: Danh sách phòng ban lấy đúng thứ tự theo tên.
        Department department = Department.builder().departmentId(3).build();
        when(departments.findAll(Sort.by("departmentName"))).thenReturn(List.of(department));

        // When: Gọi service với dữ liệu đã chuẩn bị.
        List<Department> result = service.findDepartments();

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(List.of(department), result);
        verify(departments, times(1)).findAll(Sort.by("departmentName"));
    }

    @Test
    @DisplayName("ACCOUNT-LIST-10 | Deactivate account không tồn tại: trả 404 và không lưu")
    void deactivate_missingAccount_returnsNotFoundWithoutSaving() {
        // Given: Deactivate account không tồn tại: trả 404 và không lưu.
        // Repository trả Optional.empty cho user ID 77.

        // When: Gọi service với dữ liệu đã chuẩn bị.
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.deactivate(77));

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(404, error.getStatusCode().value());
        verify(users, times(0)).save(any(User.class));
    }

}

