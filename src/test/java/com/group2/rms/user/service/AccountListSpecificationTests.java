package com.group2.rms.user.service;

import com.group2.rms.candidate.repository.CandidateRepository;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit test Criteria được Specification của service tạo ra; không chạy JPA/SQL.
 * Captor chỉ lấy đối số Specification/Predicate[] của mock. Path sâu tối đa hai cấp.
 */
@SuppressWarnings("unchecked")
class AccountListSpecificationTests {
    private final UserRepository users = mock(UserRepository.class);
    private final AccountListService service = new AccountListService(users, mock(RoleRepository.class),
            mock(DepartmentRepository.class), mock(CandidateRepository.class));
    private final Root<User> root = mock(Root.class);
    private final CriteriaBuilder builder = mock(CriteriaBuilder.class);
    private final Path<Object> role = mock(Path.class);
    private final Path<Object> roleName = mock(Path.class);
    private final Predicate rolePredicate = mock(Predicate.class);
    private final Predicate combinedPredicate = mock(Predicate.class);
    private final Set<String> internalRoles = Set.of("System Admin", "HR", "Hiring Manager", "Director", "Interviewer");
    private final ArgumentCaptor<Specification<User>> specification = ArgumentCaptor.forClass(Specification.class);
    private final ArgumentCaptor<Predicate[]> andClauses = ArgumentCaptor.forClass(Predicate[].class);
    private final ArgumentCaptor<Predicate[]> orClauses = ArgumentCaptor.forClass(Predicate[].class);

    @BeforeEach
    void setUp() {
        when(users.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        when(root.get("role")).thenReturn(role);
        when(role.get("roleName")).thenReturn(roleName);
        when(roleName.in(internalRoles)).thenReturn(rolePredicate);
        when(builder.and(any(Predicate[].class))).thenReturn(combinedPredicate);
    }

    @Test
    @DisplayName("SPEC-ACCOUNT-01 | Danh sách nội bộ luôn giới hạn năm role nội bộ")
    void internalScope_withoutOptionalFilters_keepsOnlyInternalRoles() {
        // Given: SPEC-ACCOUNT-01 | Danh sách nội bộ luôn giới hạn năm role nội bộ.
        // Keyword, roleId, departmentId và status đều null; không có filter tùy chọn.

        // When: Service tạo Specification; gọi toPredicate thật trên các Criteria mock.
        service.findAccounts(null, null, null, null, "name", 0);
        verify(users, times(1)).findAll(specification.capture(), any(Pageable.class));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra bộ điều kiện AND/OR và đối số filter; không phụ thuộc thứ tự các nhánh AND.
        assertEquals(combinedPredicate, actual);
        verify(builder, times(1)).and(andClauses.capture());
        assertEquals(Set.of(rolePredicate), Set.of(andClauses.getValue()));
        verify(roleName, times(1)).in(internalRoles);
        verify(builder, times(0)).equal(roleName, "Candidate");
        verify(builder, times(0)).or(any(Predicate[].class));
    }

    @Test
    @DisplayName("SPEC-ACCOUNT-02 | Danh sách Candidate dùng role Candidate, không dùng nhóm nội bộ")
    void candidateScope_usesExactCandidateRole() {
        // Given: SPEC-ACCOUNT-02 | Danh sách Candidate dùng role Candidate, không dùng nhóm nội bộ.
        when(builder.equal(roleName, "Candidate")).thenReturn(rolePredicate);

        // When: Service tạo Specification; gọi toPredicate thật trên các Criteria mock.
        service.findCandidateAccounts(null, null, "name", 0);
        verify(users, times(1)).findAll(specification.capture(), any(Pageable.class));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra bộ điều kiện AND/OR và đối số filter; không phụ thuộc thứ tự các nhánh AND.
        assertEquals(combinedPredicate, actual);
        verify(builder, times(1)).and(andClauses.capture());
        assertEquals(Set.of(rolePredicate), Set.of(andClauses.getValue()));
        verify(builder, times(1)).equal(roleName, "Candidate");
        verify(roleName, times(0)).in(anyCollection());
    }

    @Test
    @DisplayName("SPEC-ACCOUNT-03 | roleId hoạt động độc lập khi departmentId null")
    void roleId_withoutDepartment_filtersRoleIndependently() {
        // Given: SPEC-ACCOUNT-03 | roleId hoạt động độc lập khi departmentId null.
        Path<Object> roleId = mock(Path.class);
        Predicate roleFilter = mock(Predicate.class);
        when(role.get("roleId")).thenReturn(roleId);
        when(builder.equal(roleId, 2)).thenReturn(roleFilter);

        // When: Service tạo Specification; gọi toPredicate thật trên các Criteria mock.
        service.findAccounts(null, 2, null, null, "name", 0);
        verify(users, times(1)).findAll(specification.capture(), any(Pageable.class));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra bộ điều kiện AND/OR và đối số filter; không phụ thuộc thứ tự các nhánh AND.
        assertEquals(combinedPredicate, actual);
        verify(builder, times(1)).and(andClauses.capture());
        assertEquals(Set.of(rolePredicate, roleFilter), Set.of(andClauses.getValue()));
        verify(builder, times(1)).equal(roleId, 2);
    }

    @Test
    @DisplayName("SPEC-ACCOUNT-04 | departmentId hoạt động độc lập khi roleId null")
    void departmentId_withoutRole_filtersDepartmentIndependently() {
        // Given: SPEC-ACCOUNT-04 | departmentId hoạt động độc lập khi roleId null.
        Path<Object> department = mock(Path.class);
        Path<Object> departmentId = mock(Path.class);
        Predicate departmentFilter = mock(Predicate.class);
        when(root.get("department")).thenReturn(department);
        when(department.get("departmentId")).thenReturn(departmentId);
        when(builder.equal(departmentId, 7)).thenReturn(departmentFilter);

        // When: Service tạo Specification; gọi toPredicate thật trên các Criteria mock.
        service.findAccounts(null, null, 7, null, "name", 0);
        verify(users, times(1)).findAll(specification.capture(), any(Pageable.class));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra bộ điều kiện AND/OR và đối số filter; không phụ thuộc thứ tự các nhánh AND.
        assertEquals(combinedPredicate, actual);
        verify(builder, times(1)).and(andClauses.capture());
        assertEquals(Set.of(rolePredicate, departmentFilter), Set.of(andClauses.getValue()));
        verify(builder, times(1)).equal(departmentId, 7);
    }

    @Test
    @DisplayName("SPEC-ACCOUNT-05 | role, department và Active phải AND cùng phạm vi nội bộ")
    void roleDepartmentAndActiveStatus_areCombinedWithScope() {
        // Given: SPEC-ACCOUNT-05 | role, department và Active phải AND cùng phạm vi nội bộ.
        Path<Object> roleId = mock(Path.class);
        Predicate roleFilter = mock(Predicate.class);
        when(role.get("roleId")).thenReturn(roleId);
        when(builder.equal(roleId, 2)).thenReturn(roleFilter);
        Path<Object> department = mock(Path.class);
        Path<Object> departmentId = mock(Path.class);
        Predicate departmentFilter = mock(Predicate.class);
        when(root.get("department")).thenReturn(department);
        when(department.get("departmentId")).thenReturn(departmentId);
        when(builder.equal(departmentId, 7)).thenReturn(departmentFilter);
        Path<Object> status = mock(Path.class);
        Predicate statusFilter = mock(Predicate.class);
        when(root.get("accountStatus")).thenReturn(status);
        when(builder.equal(status, "Active")).thenReturn(statusFilter);

        // When: Service tạo Specification; gọi toPredicate thật trên các Criteria mock.
        service.findAccounts(null, 2, 7, "Active", "name", 0);
        verify(users, times(1)).findAll(specification.capture(), any(Pageable.class));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra bộ điều kiện AND/OR và đối số filter; không phụ thuộc thứ tự các nhánh AND.
        assertEquals(combinedPredicate, actual);
        verify(builder, times(1)).and(andClauses.capture());
        assertEquals(Set.of(rolePredicate, roleFilter, departmentFilter, statusFilter), Set.of(andClauses.getValue()));
        verify(builder, times(1)).equal(roleId, 2);
        verify(builder, times(1)).equal(departmentId, 7);
        verify(builder, times(1)).equal(status, "Active");
    }

    @Test
    @DisplayName("SPEC-ACCOUNT-06 | Trạng thái Inactive được thêm vào bộ lọc")
    void statusInactive_addsExactStatusPredicate() {
        // Given: SPEC-ACCOUNT-06 | Trạng thái Inactive được thêm vào bộ lọc.
        Path<Object> status = mock(Path.class);
        Predicate statusFilter = mock(Predicate.class);
        when(root.get("accountStatus")).thenReturn(status);
        when(builder.equal(status, "Inactive")).thenReturn(statusFilter);

        // When: Service tạo Specification; gọi toPredicate thật trên các Criteria mock.
        service.findAccounts(null, null, null, "Inactive", "name", 0);
        verify(users, times(1)).findAll(specification.capture(), any(Pageable.class));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra bộ điều kiện AND/OR và đối số filter; không phụ thuộc thứ tự các nhánh AND.
        assertEquals(combinedPredicate, actual);
        verify(builder, times(1)).and(andClauses.capture());
        assertEquals(Set.of(rolePredicate, statusFilter), Set.of(andClauses.getValue()));
        verify(builder, times(1)).equal(status, "Inactive");
    }

    @Test
    @DisplayName("SPEC-ACCOUNT-07 | Trạng thái Blocked được thêm vào bộ lọc")
    void statusBlocked_addsExactStatusPredicate() {
        // Given: SPEC-ACCOUNT-07 | Trạng thái Blocked được thêm vào bộ lọc.
        Path<Object> status = mock(Path.class);
        Predicate statusFilter = mock(Predicate.class);
        when(root.get("accountStatus")).thenReturn(status);
        when(builder.equal(status, "Blocked")).thenReturn(statusFilter);

        // When: Service tạo Specification; gọi toPredicate thật trên các Criteria mock.
        service.findAccounts(null, null, null, "Blocked", "name", 0);
        verify(users, times(1)).findAll(specification.capture(), any(Pageable.class));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra bộ điều kiện AND/OR và đối số filter; không phụ thuộc thứ tự các nhánh AND.
        assertEquals(combinedPredicate, actual);
        verify(builder, times(1)).and(andClauses.capture());
        assertEquals(Set.of(rolePredicate, statusFilter), Set.of(andClauses.getValue()));
        verify(builder, times(1)).equal(status, "Blocked");
    }

    @Test
    @DisplayName("SPEC-ACCOUNT-08 | Status Deleted không tạo điều kiện trạng thái")
    void statusUnknown_isIgnored() {
        // Given: SPEC-ACCOUNT-08 | Status Deleted không tạo điều kiện trạng thái.
        // Status không thuộc Active, Inactive, Blocked.

        // When: Service tạo Specification; gọi toPredicate thật trên các Criteria mock.
        service.findAccounts(null, null, null, "Deleted", "name", 0);
        verify(users, times(1)).findAll(specification.capture(), any(Pageable.class));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra bộ điều kiện AND/OR và đối số filter; không phụ thuộc thứ tự các nhánh AND.
        assertEquals(combinedPredicate, actual);
        verify(builder, times(1)).and(andClauses.capture());
        assertEquals(Set.of(rolePredicate), Set.of(andClauses.getValue()));
        verify(root, times(0)).get("accountStatus");
    }

    @Test
    @DisplayName("SPEC-ACCOUNT-12 | Status trống không tạo điều kiện trạng thái")
    void statusBlank_isIgnored() {
        // Given: SPEC-ACCOUNT-12 | Status trống không tạo điều kiện trạng thái.
        // Status không thuộc Active, Inactive, Blocked.

        // When: Service tạo Specification; gọi toPredicate thật trên các Criteria mock.
        service.findAccounts(null, null, null, "  ", "name", 0);
        verify(users, times(1)).findAll(specification.capture(), any(Pageable.class));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra bộ điều kiện AND/OR và đối số filter; không phụ thuộc thứ tự các nhánh AND.
        assertEquals(combinedPredicate, actual);
        verify(builder, times(1)).and(andClauses.capture());
        assertEquals(Set.of(rolePredicate), Set.of(andClauses.getValue()));
        verify(root, times(0)).get("accountStatus");
    }

    @Test
    @DisplayName("SPEC-ACCOUNT-09 | Keyword trim/lowercase và escape backslash, underscore, percent ở cả ba trường")
    void keyword_escapesWildcardsAndSearchesAllThreeColumns() {
        // Given: SPEC-ACCOUNT-09 | Keyword trim/lowercase và escape backslash, underscore, percent ở cả ba trường.
        String expectedPattern = "%user" + "\\\\" + "\\_" + "\\%" + "%";
        Path<String> fullName = mock(Path.class);
        Path<String> email = mock(Path.class);
        Path<String> username = mock(Path.class);
        Predicate byFullName = mock(Predicate.class);
        Predicate byEmail = mock(Predicate.class);
        Predicate byUsername = mock(Predicate.class);
        Predicate searchPredicate = mock(Predicate.class);
        when(root.<String>get("fullName")).thenReturn(fullName);
        when(root.<String>get("email")).thenReturn(email);
        when(root.<String>get("username")).thenReturn(username);
        when(builder.lower(fullName)).thenReturn(fullName);
        when(builder.lower(email)).thenReturn(email);
        when(builder.lower(username)).thenReturn(username);
        when(builder.like(fullName, expectedPattern, '\\')).thenReturn(byFullName);
        when(builder.like(email, expectedPattern, '\\')).thenReturn(byEmail);
        when(builder.like(username, expectedPattern, '\\')).thenReturn(byUsername);
        when(builder.or(any(Predicate[].class))).thenReturn(searchPredicate);

        // When: Service tạo Specification; gọi toPredicate thật trên các Criteria mock.
        service.findAccounts("  USER\\_%  ", null, null, null, "name", 0);
        verify(users, times(1)).findAll(specification.capture(), any(Pageable.class));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra bộ điều kiện AND/OR và đối số filter; không phụ thuộc thứ tự các nhánh AND.
        assertEquals(combinedPredicate, actual);
        verify(builder, times(1)).and(andClauses.capture());
        assertEquals(Set.of(rolePredicate, searchPredicate), Set.of(andClauses.getValue()));
        verify(builder, times(1)).lower(fullName);
        verify(builder, times(1)).lower(email);
        verify(builder, times(1)).lower(username);
        verify(builder, times(1)).like(fullName, expectedPattern, '\\');
        verify(builder, times(1)).like(email, expectedPattern, '\\');
        verify(builder, times(1)).like(username, expectedPattern, '\\');
        verify(builder, times(1)).or(orClauses.capture());
        assertEquals(Set.of(byFullName, byEmail, byUsername), Set.of(orClauses.getValue()));
    }

    @Test
    @DisplayName("SPEC-ACCOUNT-10 | Keyword toàn khoảng trắng không tạo điều kiện tìm kiếm")
    void whitespaceKeyword_doesNotCreateSearchPredicate() {
        // Given: SPEC-ACCOUNT-10 | Keyword toàn khoảng trắng không tạo điều kiện tìm kiếm.
        // trim biến keyword thành chuỗi rỗng.

        // When: Service tạo Specification; gọi toPredicate thật trên các Criteria mock.
        service.findAccounts("   ", null, null, null, "name", 0);
        verify(users, times(1)).findAll(specification.capture(), any(Pageable.class));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra bộ điều kiện AND/OR và đối số filter; không phụ thuộc thứ tự các nhánh AND.
        assertEquals(combinedPredicate, actual);
        verify(builder, times(1)).and(andClauses.capture());
        assertEquals(Set.of(rolePredicate), Set.of(andClauses.getValue()));
        verify(builder, times(0)).lower(any());
        verify(builder, times(0)).or(any(Predicate[].class));
    }

    @Test
    @DisplayName("SPEC-ACCOUNT-11 | Tìm kiếm Candidate vẫn AND role Candidate và trạng thái Blocked")
    void candidateKeywordAndBlockedStatus_keepCandidateRestriction() {
        // Given: SPEC-ACCOUNT-11 | Tìm kiếm Candidate vẫn AND role Candidate và trạng thái Blocked.
        when(builder.equal(roleName, "Candidate")).thenReturn(rolePredicate);
        Path<Object> status = mock(Path.class);
        Predicate statusFilter = mock(Predicate.class);
        when(root.get("accountStatus")).thenReturn(status);
        when(builder.equal(status, "Blocked")).thenReturn(statusFilter);
        String expectedPattern = "%email%";
        Path<String> fullName = mock(Path.class);
        Path<String> email = mock(Path.class);
        Path<String> username = mock(Path.class);
        Predicate byFullName = mock(Predicate.class);
        Predicate byEmail = mock(Predicate.class);
        Predicate byUsername = mock(Predicate.class);
        Predicate searchPredicate = mock(Predicate.class);
        when(root.<String>get("fullName")).thenReturn(fullName);
        when(root.<String>get("email")).thenReturn(email);
        when(root.<String>get("username")).thenReturn(username);
        when(builder.lower(fullName)).thenReturn(fullName);
        when(builder.lower(email)).thenReturn(email);
        when(builder.lower(username)).thenReturn(username);
        when(builder.like(fullName, expectedPattern, '\\')).thenReturn(byFullName);
        when(builder.like(email, expectedPattern, '\\')).thenReturn(byEmail);
        when(builder.like(username, expectedPattern, '\\')).thenReturn(byUsername);
        when(builder.or(any(Predicate[].class))).thenReturn(searchPredicate);

        // When: Service tạo Specification; gọi toPredicate thật trên các Criteria mock.
        service.findCandidateAccounts("  EMAIL  ", "Blocked", "name", 0);
        verify(users, times(1)).findAll(specification.capture(), any(Pageable.class));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra bộ điều kiện AND/OR và đối số filter; không phụ thuộc thứ tự các nhánh AND.
        assertEquals(combinedPredicate, actual);
        verify(builder, times(1)).and(andClauses.capture());
        assertEquals(Set.of(rolePredicate, searchPredicate, statusFilter), Set.of(andClauses.getValue()));
        verify(builder, times(1)).equal(roleName, "Candidate");
        verify(builder, times(1)).equal(status, "Blocked");
        verify(roleName, times(0)).in(anyCollection());
        verify(builder, times(1)).lower(fullName);
        verify(builder, times(1)).lower(email);
        verify(builder, times(1)).lower(username);
        verify(builder, times(1)).like(fullName, expectedPattern, '\\');
        verify(builder, times(1)).like(email, expectedPattern, '\\');
        verify(builder, times(1)).like(username, expectedPattern, '\\');
        verify(builder, times(1)).or(orClauses.capture());
        assertEquals(Set.of(byFullName, byEmail, byUsername), Set.of(orClauses.getValue()));
    }
}
