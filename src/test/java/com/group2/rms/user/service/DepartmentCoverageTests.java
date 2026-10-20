package com.group2.rms.user.service;

import com.group2.rms.user.dto.*;
import com.group2.rms.user.entity.*;
import com.group2.rms.user.exception.DepartmentFieldException;
import com.group2.rms.user.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DepartmentCoverageTests {
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final DepartmentService service = new DepartmentService(departments, users);
    private final Department department = Department.builder().departmentId(7).departmentName("Engineering")
            .departmentStatus("Active").build();

    @Test
    @DisplayName("DEPARTMENT-UNIT-01 | Tên phòng ban Null: trả lỗi và không lưu")
    void create_nameNull_rejectsWithoutSaving() {
        // Given: Tên phòng ban Null: trả lỗi và không lưu.
        DepartmentRequest input = new DepartmentRequest(null, null);

        // When: Gọi service với dữ liệu đã chuẩn bị.
        DepartmentFieldException error = assertThrows(DepartmentFieldException.class, () -> service.create(input));

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals("departmentName", error.getField());
        verify(departments, times(0)).saveAndFlush(any(Department.class));
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-02 | Tên phòng ban Empty: trả lỗi và không lưu")
    void create_nameEmpty_rejectsWithoutSaving() {
        // Given: Tên phòng ban Empty: trả lỗi và không lưu.
        DepartmentRequest input = new DepartmentRequest("", null);

        // When: Gọi service với dữ liệu đã chuẩn bị.
        DepartmentFieldException error = assertThrows(DepartmentFieldException.class, () -> service.create(input));

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals("departmentName", error.getField());
        verify(departments, times(0)).saveAndFlush(any(Department.class));
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-03 | Tên phòng ban Blank: trả lỗi và không lưu")
    void create_nameBlank_rejectsWithoutSaving() {
        // Given: Tên phòng ban Blank: trả lỗi và không lưu.
        DepartmentRequest input = new DepartmentRequest("  ", null);

        // When: Gọi service với dữ liệu đã chuẩn bị.
        DepartmentFieldException error = assertThrows(DepartmentFieldException.class, () -> service.create(input));

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals("departmentName", error.getField());
        verify(departments, times(0)).saveAndFlush(any(Department.class));
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-04 | Tên phòng ban đúng 1 ký tự: chấp nhận và giữ ID")
    void update_nameLength1_acceptsBoundary() {
        // Given: Tên phòng ban đúng 1 ký tự: chấp nhận và giữ ID.
        when(departments.findById(7)).thenReturn(Optional.of(department));

        // When: Gọi service với dữ liệu đã chuẩn bị.
        service.update(7, new DepartmentRequest("x".repeat(1), null));

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals("x".repeat(1), department.getDepartmentName());
        assertEquals(7, department.getDepartmentId());
        assertEquals("Active", department.getDepartmentStatus());
        verify(departments, times(1)).saveAndFlush(department);
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-05 | Tên phòng ban đúng 100 ký tự: chấp nhận và giữ ID")
    void update_nameLength100_acceptsBoundary() {
        // Given: Tên phòng ban đúng 100 ký tự: chấp nhận và giữ ID.
        when(departments.findById(7)).thenReturn(Optional.of(department));

        // When: Gọi service với dữ liệu đã chuẩn bị.
        service.update(7, new DepartmentRequest("x".repeat(100), null));

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals("x".repeat(100), department.getDepartmentName());
        assertEquals(7, department.getDepartmentId());
        assertEquals("Active", department.getDepartmentStatus());
        verify(departments, times(1)).saveAndFlush(department);
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-06 | Không có trưởng phòng: trả đúng thông tin manager")
    void findForEdit_withoutManager_mapsCurrentValues() {
        // Given: Không có trưởng phòng: trả đúng thông tin manager.
        when(departments.findById(7)).thenReturn(Optional.of(department));

        // When: Gọi service với dữ liệu đã chuẩn bị.
        DepartmentResponse result = service.findForEdit(7);

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(7, result.id());
        assertEquals("Engineering", result.name());
        assertEquals(null, result.managerId());
        assertEquals(null, result.managerName());
        assertEquals(null, result.managerUsername());
        // Trạng thái hiển thị phải được map từ department, không bị mất thành null.
        assertEquals("Active", result.status());
        verify(departments, times(0)).saveAndFlush(any(Department.class));
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-07 | Có trưởng phòng: trả đúng thông tin manager")
    void findForEdit_withManager_mapsCurrentValues() {
        // Given: Có trưởng phòng: trả đúng thông tin manager.
        department.setManager(User.builder().userId(9).fullName("Manager").username("manager9").build());
        when(departments.findById(7)).thenReturn(Optional.of(department));

        // When: Gọi service với dữ liệu đã chuẩn bị.
        DepartmentResponse result = service.findForEdit(7);

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(7, result.id());
        assertEquals("Engineering", result.name());
        assertEquals(Integer.valueOf(9), result.managerId());
        assertEquals("Manager", result.managerName());
        assertEquals("manager9", result.managerUsername());
        assertEquals("Active", result.status());
        verify(departments, times(0)).saveAndFlush(any(Department.class));
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-08 | Manager role System Admin Active và cùng phòng ban: được gán, role giữ nguyên")
    void update_activeAdminInSameDepartment_assignsManager() {
        // Given: Manager role System Admin Active và cùng phòng ban: được gán, role giữ nguyên.
        User manager = User.builder().userId(9).role(Role.builder().roleName("System Admin").build())
            .accountStatus("Active").department(department).build();
        when(departments.findById(7)).thenReturn(Optional.of(department));
        when(users.findById(9)).thenReturn(Optional.of(manager));

        // When: Gọi service với dữ liệu đã chuẩn bị.
        service.update(7, new DepartmentRequest("New Department", 9));

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(manager, department.getManager());
        assertEquals("New Department", department.getDepartmentName());
        assertEquals("System Admin", manager.getRole().getRoleName());
        verify(departments, times(1)).saveAndFlush(department);
        verify(users, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-09 | Manager role HR Active và cùng phòng ban: được gán, role giữ nguyên")
    void update_activeHRInSameDepartment_assignsManager() {
        // Given: Manager role HR Active và cùng phòng ban: được gán, role giữ nguyên.
        User manager = User.builder().userId(9).role(Role.builder().roleName("HR").build())
            .accountStatus("Active").department(department).build();
        when(departments.findById(7)).thenReturn(Optional.of(department));
        when(users.findById(9)).thenReturn(Optional.of(manager));

        // When: Gọi service với dữ liệu đã chuẩn bị.
        service.update(7, new DepartmentRequest("New Department", 9));

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(manager, department.getManager());
        assertEquals("New Department", department.getDepartmentName());
        assertEquals("HR", manager.getRole().getRoleName());
        verify(departments, times(1)).saveAndFlush(department);
        verify(users, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-10 | Manager role Hiring Manager Active và cùng phòng ban: được gán, role giữ nguyên")
    void update_activeHMInSameDepartment_assignsManager() {
        // Given: Manager role Hiring Manager Active và cùng phòng ban: được gán, role giữ nguyên.
        User manager = User.builder().userId(9).role(Role.builder().roleName("Hiring Manager").build())
            .accountStatus("Active").department(department).build();
        when(departments.findById(7)).thenReturn(Optional.of(department));
        when(users.findById(9)).thenReturn(Optional.of(manager));

        // When: Gọi service với dữ liệu đã chuẩn bị.
        service.update(7, new DepartmentRequest("New Department", 9));

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(manager, department.getManager());
        assertEquals("New Department", department.getDepartmentName());
        assertEquals("Hiring Manager", manager.getRole().getRoleName());
        verify(departments, times(1)).saveAndFlush(department);
        verify(users, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-11 | Manager role Director Active và cùng phòng ban: được gán, role giữ nguyên")
    void update_activeDirectorInSameDepartment_assignsManager() {
        // Given: Manager role Director Active và cùng phòng ban: được gán, role giữ nguyên.
        User manager = User.builder().userId(9).role(Role.builder().roleName("Director").build())
            .accountStatus("Active").department(department).build();
        when(departments.findById(7)).thenReturn(Optional.of(department));
        when(users.findById(9)).thenReturn(Optional.of(manager));

        // When: Gọi service với dữ liệu đã chuẩn bị.
        service.update(7, new DepartmentRequest("New Department", 9));

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(manager, department.getManager());
        assertEquals("New Department", department.getDepartmentName());
        assertEquals("Director", manager.getRole().getRoleName());
        verify(departments, times(1)).saveAndFlush(department);
        verify(users, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-12 | Manager role Interviewer Active và cùng phòng ban: được gán, role giữ nguyên")
    void update_activeInterviewerInSameDepartment_assignsManager() {
        // Given: Manager role Interviewer Active và cùng phòng ban: được gán, role giữ nguyên.
        User manager = User.builder().userId(9).role(Role.builder().roleName("Interviewer").build())
            .accountStatus("Active").department(department).build();
        when(departments.findById(7)).thenReturn(Optional.of(department));
        when(users.findById(9)).thenReturn(Optional.of(manager));

        // When: Gọi service với dữ liệu đã chuẩn bị.
        service.update(7, new DepartmentRequest("New Department", 9));

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(manager, department.getManager());
        assertEquals("New Department", department.getDepartmentName());
        assertEquals("Interviewer", manager.getRole().getRoleName());
        verify(departments, times(1)).saveAndFlush(department);
        verify(users, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-13 | Manager không thuộc phòng ban nào: từ chối, không đổi tên hoặc lưu")
    void update_managerWithoutDepartment_rejectsBeforeMutation() {
        // Given: Manager không thuộc phòng ban nào: từ chối, không đổi tên hoặc lưu.
        User manager = User.builder().userId(9).role(Role.builder().roleName("HR").build()).accountStatus("Active").build();
        when(departments.findById(7)).thenReturn(Optional.of(department));
        when(users.findById(9)).thenReturn(Optional.of(manager));

        // When: Gọi service với dữ liệu đã chuẩn bị.
        DepartmentFieldException error = assertThrows(DepartmentFieldException.class,
            () -> service.update(7, new DepartmentRequest("Changed", 9)));

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals("managerId", error.getField());
        assertEquals("Engineering", department.getDepartmentName());
        assertEquals(null, department.getManager());
        verify(departments, times(0)).saveAndFlush(any(Department.class));
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-14 | Tên trùng phòng ban khác: không sửa dữ liệu hiện tại")
    void update_duplicateName_rejectsBeforeMutation() {
        // Given: Tên trùng phòng ban khác: không sửa dữ liệu hiện tại.
        when(departments.findById(7)).thenReturn(Optional.of(department));
        when(departments.existsByDepartmentNameIgnoreCaseAndDepartmentIdNot("Taken", 7)).thenReturn(true);

        // When: Gọi service với dữ liệu đã chuẩn bị.
        DepartmentFieldException error = assertThrows(DepartmentFieldException.class,
            () -> service.update(7, new DepartmentRequest("Taken", null)));

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals("departmentName", error.getField());
        assertEquals("Engineering", department.getDepartmentName());
        verify(departments, times(0)).saveAndFlush(any(Department.class));
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-15 | SQL duplicate 2601: chuyển thành lỗi field tên phòng ban")
    void create_sqlDuplicate2601_returnsNameFieldError() {
        // Given: SQL duplicate 2601: chuyển thành lỗi field tên phòng ban.
        when(departments.saveAndFlush(any(Department.class))).thenThrow(
            new DataIntegrityViolationException("write", new SQLException("duplicate", "23000", 2601)));

        // When: Gọi service với dữ liệu đã chuẩn bị.
        DepartmentFieldException error = assertThrows(DepartmentFieldException.class,
            () -> service.create(new DepartmentRequest("Name", null)));

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals("departmentName", error.getField());
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-16 | SQL duplicate 2627: chuyển thành lỗi field tên phòng ban")
    void create_sqlDuplicate2627_returnsNameFieldError() {
        // Given: SQL duplicate 2627: chuyển thành lỗi field tên phòng ban.
        when(departments.saveAndFlush(any(Department.class))).thenThrow(
            new DataIntegrityViolationException("write", new SQLException("duplicate", "23000", 2627)));

        // When: Gọi service với dữ liệu đã chuẩn bị.
        DepartmentFieldException error = assertThrows(DepartmentFieldException.class,
            () -> service.create(new DepartmentRequest("Name", null)));

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals("departmentName", error.getField());
    }

    @Test
    @DisplayName("DEPARTMENT-PIT-01 | Sửa phòng gặp duplicate: lỗi giữ ID và form để hiển thị lại")
    void update_sqlDuplicate_preservesDepartmentIdAndSubmittedForm() {
        // Given: Phòng 7 tồn tại; repository mock báo duplicate khi lưu form sửa phòng.
        DepartmentRequest input = new DepartmentRequest("New name", null);
        when(departments.findById(7)).thenReturn(Optional.of(department));
        when(departments.saveAndFlush(department)).thenThrow(
                new DataIntegrityViolationException("write", new SQLException("duplicate", "23000", 2627)));

        // When: Sửa phòng, chỉ mô phỏng exception của repository; không chạy SQL/database.
        DepartmentFieldException error = assertThrows(DepartmentFieldException.class,
                () -> service.update(7, input));

        // Then: Thông tin lỗi giữ đúng field, ID phòng và form đã nhập để người dùng sửa lại.
        assertEquals("departmentName", error.getField());
        assertEquals(7, error.getDepartmentId());
        assertEquals(input, error.getForm());
        verify(departments, times(1)).saveAndFlush(department);
        verify(users, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-17 | Lỗi integrity không phải duplicate SQL: giữ nguyên exception")
    void create_integrityFailureWithoutSqlCause_preservesOriginalError() {
        // Given: Lỗi integrity không phải duplicate SQL: giữ nguyên exception.
        DataIntegrityViolationException failure = new DataIntegrityViolationException("unknown constraint");
        when(departments.saveAndFlush(any(Department.class))).thenThrow(failure);

        // When: Gọi service với dữ liệu đã chuẩn bị.
        DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
            () -> service.create(new DepartmentRequest("Name", null)));

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(failure, error);
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-18 | List phòng ban với page âm: vẫn trả dữ liệu, không ghi DB")
    void findDepartments_negativePage_returnsRowsWithoutSaving() {
        // Given: List phòng ban với page âm: vẫn trả dữ liệu, không ghi DB.
        when(departments.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(department)));

        // When: Gọi service với dữ liệu đã chuẩn bị.
        Page<DepartmentResponse> result = service.findDepartments(" Engineering ", "Active", -1);

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(1, result.getNumberOfElements());
        assertEquals("Engineering", result.getContent().getFirst().name());
        verify(departments, times(1)).findAll(any(Specification.class), eq(PageRequest.of(0, 10, Sort.by("departmentName", "departmentId"))));
        verify(departments, times(0)).saveAndFlush(any(Department.class));
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-19 | Search null và không có dữ liệu: trả trang rỗng")
    void findDepartments_nullSearch_returnsEmptyPage() {
        // Given: Search null và không có dữ liệu: trả trang rỗng.
        when(departments.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        // When: Gọi service với dữ liệu đã chuẩn bị.
        Page<DepartmentResponse> result = service.findDepartments(null, null, 0);

        // Then: Kiểm tra dữ liệu thực tế và số lần gọi dependency.
        assertEquals(true, result.isEmpty());
        verify(departments, times(0)).saveAndFlush(any(Department.class));
    }

    @Test
    @DisplayName("DEPARTMENT-UNIT-20 | Ghi nhận hiện trạng: đổi manager không đồng bộ role cũ và mới")
    void update_replaceManager_currentBehaviorLeavesBothRolesUnchanged() {
        // Given: Manager cũ có role HM, người được thay thế đang là Interviewer trong cùng phòng.
        Role hmRole = Role.builder().roleName("Hiring Manager").build();
        Role interviewerRole = Role.builder().roleName("Interviewer").build();
        User oldManager = User.builder().userId(1).role(hmRole).department(department).accountStatus("Active").build();
        User newManager = User.builder().userId(2).role(interviewerRole).department(department).accountStatus("Active").build();
        department.setManager(oldManager);
        when(departments.findById(7)).thenReturn(Optional.of(department));
        when(users.findById(2)).thenReturn(Optional.of(newManager));
        // When: Đổi người quản lý phòng sang user 2 qua service hiện tại.
        service.update(7, new DepartmentRequest("Engineering", 2));
        // Then: Đây là hiện trạng cần báo cáo: chỉ đổi manager; hai role vẫn giữ nguyên.
        // Ca này mô tả source hiện tại, không khẳng định hành vi đồng bộ role đã đúng nghiệp vụ.
        assertEquals(newManager, department.getManager());
        assertEquals("Hiring Manager", oldManager.getRole().getRoleName());
        assertEquals("Interviewer", newManager.getRole().getRoleName());
        verify(departments, times(1)).saveAndFlush(department);
    }
}
