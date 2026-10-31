package com.group2.rms.user.service;

import com.group2.rms.user.entity.Department;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.repository.RoleRepository;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Chỉ kiểm tra các lời gọi Criteria/Predicate thật từ Specification; không dùng SQL provider.
 */
@SuppressWarnings("unchecked")
class DepartmentSpecificationTests {
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final DepartmentService service = new DepartmentService(departments, mock(UserRepository.class), mock(RoleRepository.class), org.mockito.Mockito.mock(com.group2.rms.user.service.UserAuditLogger.class));
    private final Root<Department> root = mock(Root.class);
    private final CriteriaBuilder builder = mock(CriteriaBuilder.class);
    private final Path<String> departmentName = mock(Path.class);
    private final Predicate byName = mock(Predicate.class);
    private final Predicate byStatus = mock(Predicate.class);
    private final Predicate combinedPredicate = mock(Predicate.class);
    private final ArgumentCaptor<Specification<Department>> specification = ArgumentCaptor.forClass(Specification.class);

    @BeforeEach
    void setUp() {
        when(departments.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        when(root.<String>get("departmentName")).thenReturn(departmentName);
        when(builder.lower(departmentName)).thenReturn(departmentName);
    }

    @Test
    @DisplayName("SPEC-DEPARTMENT-01 | Search/status null dùng pattern phần trăm và không thêm status")
    void nullSearchAndStatus_keepOnlyNamePredicate() {
        // Given: SPEC-DEPARTMENT-01 | Search/status null dùng pattern phần trăm và không thêm status.
        when(builder.like(departmentName, "%%", '\\')).thenReturn(byName);

        // When: Lấy Specification của service và gọi toPredicate thật, không dùng database.
        service.findDepartments(null, null, 0);
        verify(departments, times(1)).findAll(specification.capture(), any(Pageable.class));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra pattern, escape character và status trong biểu thức Criteria.
        assertEquals(byName, actual);
        verify(builder, times(1)).lower(departmentName);
        verify(builder, times(1)).like(departmentName, "%%", '\\');
        verify(root, times(0)).get("departmentStatus");
        verify(builder, times(0)).and(any(Predicate.class), any(Predicate.class));
    }

    @Test
    @DisplayName("SPEC-DEPARTMENT-02 | Active được AND với bộ lọc tên phòng")
    void statusActive_isCombinedWithName() {
        // Given: SPEC-DEPARTMENT-02 | Active được AND với bộ lọc tên phòng.
        Path<Object> status = mock(Path.class);
        when(root.get("departmentStatus")).thenReturn(status);
        when(builder.like(departmentName, "%%", '\\')).thenReturn(byName);
        when(builder.equal(status, "Active")).thenReturn(byStatus);
        when(builder.and(byName, byStatus)).thenReturn(combinedPredicate);

        // When: Lấy Specification của service và gọi toPredicate thật, không dùng database.
        service.findDepartments("", "Active", 2);
        // Page dương phải được giữ nguyên; không chỉ kiểm tra trường hợp page về 0.
        verify(departments, times(1)).findAll(specification.capture(),
                eq(PageRequest.of(2, 10, Sort.by("departmentName", "departmentId"))));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra pattern, escape character và status trong biểu thức Criteria.
        assertEquals(combinedPredicate, actual);
        verify(builder, times(1)).lower(departmentName);
        verify(builder, times(1)).equal(status, "Active");
        verify(builder, times(1)).and(byName, byStatus);
    }

    @Test
    @DisplayName("SPEC-DEPARTMENT-03 | Inactive được AND với bộ lọc tên phòng")
    void statusInactive_isCombinedWithName() {
        // Given: SPEC-DEPARTMENT-03 | Inactive được AND với bộ lọc tên phòng.
        Path<Object> status = mock(Path.class);
        when(root.get("departmentStatus")).thenReturn(status);
        when(builder.like(departmentName, "%%", '\\')).thenReturn(byName);
        when(builder.equal(status, "Inactive")).thenReturn(byStatus);
        when(builder.and(byName, byStatus)).thenReturn(combinedPredicate);

        // When: Lấy Specification của service và gọi toPredicate thật, không dùng database.
        service.findDepartments("", "Inactive", 0);
        verify(departments, times(1)).findAll(specification.capture(), any(Pageable.class));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra pattern, escape character và status trong biểu thức Criteria.
        assertEquals(combinedPredicate, actual);
        verify(builder, times(1)).lower(departmentName);
        verify(builder, times(1)).equal(status, "Inactive");
        verify(builder, times(1)).and(byName, byStatus);
    }

    @Test
    @DisplayName("SPEC-DEPARTMENT-04 | Blocked không phải trạng thái phòng hợp lệ nên bị bỏ qua")
    void unsupportedStatus_isIgnored() {
        // Given: SPEC-DEPARTMENT-04 | Blocked không phải trạng thái phòng hợp lệ nên bị bỏ qua.
        when(builder.like(departmentName, "%%", '\\')).thenReturn(byName);

        // When: Lấy Specification của service và gọi toPredicate thật, không dùng database.
        service.findDepartments("", "Blocked", 0);
        verify(departments, times(1)).findAll(specification.capture(), any(Pageable.class));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra pattern, escape character và status trong biểu thức Criteria.
        assertEquals(byName, actual);
        verify(builder, times(1)).lower(departmentName);
        verify(root, times(0)).get("departmentStatus");
        verify(builder, times(0)).and(any(Predicate.class), any(Predicate.class));
    }

    @Test
    @DisplayName("SPEC-DEPARTMENT-05 | strip khoảng trắng Unicode và escape backslash, underscore, percent")
    void unicodeWhitespaceAndWildcardKeyword_areNormalized() {
        // Given: SPEC-DEPARTMENT-05 | strip khoảng trắng Unicode và escape backslash, underscore, percent.
        String expectedPattern = "%qa" + "\\\\" + "\\_" + "\\%" + "%";
        when(builder.like(departmentName, expectedPattern, '\\')).thenReturn(byName);

        // When: Lấy Specification của service và gọi toPredicate thật, không dùng database.
        service.findDepartments("\u2003QA\\_%\u2003", null, 0);
        verify(departments, times(1)).findAll(specification.capture(), any(Pageable.class));
        Predicate actual = specification.getValue().toPredicate(root, null, builder);

        // Then: Kiểm tra pattern, escape character và status trong biểu thức Criteria.
        assertEquals(byName, actual);
        verify(builder, times(1)).lower(departmentName);
        verify(builder, times(1)).like(departmentName, expectedPattern, '\\');
        verify(root, times(0)).get("departmentStatus");
        verify(builder, times(0)).and(any(Predicate.class), any(Predicate.class));
    }
}
