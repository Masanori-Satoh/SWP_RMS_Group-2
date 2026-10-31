package com.group2.rms.service;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.user.dto.DepartmentRequest;
import com.group2.rms.user.entity.*;
import com.group2.rms.user.exception.DepartmentFieldException;
import com.group2.rms.user.repository.*;
import com.group2.rms.user.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.dao.DataIntegrityViolationException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.ArgumentMatchers.any;

class DepartmentServiceTests {
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final DepartmentService service = new DepartmentService(departments, users, roles, org.mockito.Mockito.mock(com.group2.rms.user.service.UserAuditLogger.class));
    private final Department department = Department.builder().departmentId(7).departmentName("Sản phẩm").build();

    @Test
    @DisplayName("Tạo phòng: trim tên, mặc định Active, chưa có manager và trả ID được cấp")
    void createTrimsNameDefaultsActiveAndHasNoManager() {
        // Given: Repository mock mô phỏng JPA cấp ID trên chính entity được persist.
        // create() đọc lại ID từ entity đầu vào, không dùng object return của saveAndFlush().
        when(departments.saveAndFlush(any(Department.class))).thenAnswer(invocation -> {
                Department saved = invocation.getArgument(0);
                saved.setDepartmentId(8);
                return saved;
            });
        Department expected = Department.builder().departmentId(8).departmentName("Thiết kế")
            .departmentStatus("Active").manager(null).build();

        // When: Tạo phòng có khoảng trắng đầu/cuối và không chọn manager.
        int id = service.create(new DepartmentRequest("  Thiết kế  ", null));

        // Then: Đúng ID được cấp, tên trim, trạng thái Active và manager null.
        assertEquals(8, id);
        verify(departments, times(1)).saveAndFlush(refEq(expected));
        verify(users, times(0)).findById(any());
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("Tên phòng đã tồn tại kể cả Inactive: báo field departmentName và không lưu")
    void duplicateIncludesInactiveAndDoesNotPersist() {
        // Given: Tên phòng đã tồn tại kể cả Inactive.
        when(departments.existsByDepartmentNameIgnoreCase("Sản phẩm")).thenReturn(true);
        // When: Gọi service.create với dữ liệu đã chuẩn bị.
        var ex=assertThrows(DepartmentFieldException.class, () -> service.create(new DepartmentRequest("Sản phẩm", null)));
        // Then: báo field departmentName và không lưu.
        assertEquals("departmentName", ex.getField());
        verify(departments, times(0)).saveAndFlush(any());
    }

    @Test
    @DisplayName("Chặn tên phòng trống/quá 100 ký tự và không gán manager lúc tạo mới")
    void invalidLengthAndManagerAtCreateAreRejected() {
        // Given: Chặn tên phòng trống/quá 100 ký tự và không gán manager lúc tạo mới.
        // When: Gọi service.create với dữ liệu đã chuẩn bị.
        assertThrows(DepartmentFieldException.class, () -> service.create(new DepartmentRequest(" ", null)));
        // Then: Chặn tên phòng trống/quá 100 ký tự và không gán manager lúc tạo mới.
        assertThrows(DepartmentFieldException.class, () -> service.create(new DepartmentRequest("x".repeat(101), null)));
        assertEquals("managerId", assertThrows(DepartmentFieldException.class, () -> service.create(new DepartmentRequest("Legal", 9))).getField());
        verify(departments, times(0)).saveAndFlush(any());
    }

    @Test
    @DisplayName("Sửa phòng giữ tên hiện tại, chọn HR Active cùng phòng và giữ trạng thái Inactive")
    void updateOwnNameAndEligibleManagerPreservesInactiveStatus() {
        // Given: Sửa phòng giữ tên hiện tại, chọn HR Active cùng phòng và giữ trạng thái Inactive.
        department.setDepartmentStatus("Inactive");
        var manager=member(9,"HR","Active",department);
        when(departments.findForUpdate(7)).thenReturn(Optional.of(department));
        when(users.findById(9)).thenReturn(Optional.of(manager));
        when(roles.findByRoleName("Hiring Manager"))
            .thenReturn(Optional.of(Role.builder().roleId(3).roleName("Hiring Manager").build()));
        // When: Gọi service.update với dữ liệu đã chuẩn bị.
        service.update(7,new DepartmentRequest("Sản phẩm",9));
        // Then: Sửa phòng giữ tên hiện tại, chọn HR Active cùng phòng và giữ trạng thái Inactive.
        assertEquals(manager,department.getManager());
        assertEquals("Hiring Manager",manager.getRole().getRoleName());
        assertEquals("Inactive",department.getDepartmentStatus());
        verify(departments, times(1)).existsByDepartmentNameIgnoreCaseAndDepartmentIdNot("Sản phẩm",7);
    }

    @Test
    @DisplayName("Chặn manager khác phòng, Inactive, Candidate hoặc không tồn tại; giữ nguyên dữ liệu")
    void rejectsForeignInactiveCandidateAndMissingManagers() {
        // Given: Chặn manager khác phòng, Inactive, Candidate hoặc không tồn tại; giữ nguyên dữ liệu.
        when(departments.findForUpdate(7)).thenReturn(Optional.of(department));
        for (User invalid:List.of(member(9,"HR","Active",Department.builder().departmentId(8).build()),
            member(9,"HR","Inactive",department), member(9,"Candidate","Active",department))) {
            when(users.findById(9)).thenReturn(Optional.of(invalid));
            // When: Gọi service.update với dữ liệu đã chuẩn bị.
            // Then: Chặn manager khác phòng, Inactive, Candidate hoặc không tồn tại; giữ nguyên dữ liệu.
            var error=assertThrows(DepartmentFieldException.class, () -> service.update(7,new DepartmentRequest("Changed",9)));
            assertEquals("managerId",error.getField());
            // Lỗi chọn manager phải giữ ID phòng để form sửa trở về đúng phòng ban.
            assertEquals(7,error.getDepartmentId());
            assertEquals("Sản phẩm",department.getDepartmentName());
            assertEquals(null, department.getManager());
        }
        when(users.findById(9)).thenReturn(Optional.empty());
        assertThrows(DepartmentFieldException.class, () -> service.update(7,new DepartmentRequest("Changed",9)));
        verify(departments,times(0)).saveAndFlush(any());
    }

    @Test
    @DisplayName("Bỏ chọn manager khi sửa phòng: manager được đặt null")
    void managerCanBeExplicitlyCleared() {
        // Given: Bỏ chọn manager khi sửa phòng.
        var oldManager = member(9,"Hiring Manager","Active",department);
        department.setManager(oldManager);
        when(departments.findForUpdate(7)).thenReturn(Optional.of(department));
        when(roles.findByRoleName("Interviewer"))
            .thenReturn(Optional.of(Role.builder().roleId(5).roleName("Interviewer").build()));
        // When: Gọi service.update với dữ liệu đã chuẩn bị.
        service.update(7,new DepartmentRequest("Sản phẩm",null));
        // Then: manager được đặt null.
        assertEquals(null, department.getManager());
        assertEquals("Interviewer", oldManager.getRole().getRoleName());
    }

    @Test
    @DisplayName("Vô hiệu hóa/kích hoạt phòng: giữ manager/tên, không xóa và không lưu lặp")
    void deactivationAndActivationPreserveIdentityAndManagerAndAreIdempotent() {
        // Given: Vô hiệu hóa/kích hoạt phòng.
        var manager=member(9,"HR","Active",department);
        department.setManager(manager);
        when(departments.findById(7)).thenReturn(Optional.of(department));
        // When: Gọi service.deactivate với dữ liệu đã chuẩn bị.
        service.deactivate(7);
        service.deactivate(7);
        // Then: giữ manager/tên, không xóa và không lưu lặp.
        assertEquals("Inactive",department.getDepartmentStatus());
        service.activate(7);
        service.activate(7);
        assertEquals("Active",department.getDepartmentStatus());
        assertEquals(manager,department.getManager());
        assertEquals("Active",manager.getAccountStatus());
        assertEquals("Sản phẩm",department.getDepartmentName());
        verify(departments,times(2)).saveAndFlush(department);
        verify(users, times(0)).save(any());
        verify(users, times(0)).saveAndFlush(any());
        verify(users, times(0)).findById(any());
        verify(departments,times(0)).delete(any(Department.class));
        verify(departments,times(0)).deleteById(any());
    }

    @Test
    @DisplayName("SQL 2627 thành lỗi trùng tên; lỗi khóa ngoại 547 phải giữ exception")
    void onlySqlDuplicateErrorsBecomeFieldErrorsOtherFailuresEscape() {
        // Given: SQL 2627 thành lỗi trùng tên; lỗi khóa ngoại 547 phải giữ exception.
        var duplicate=new DataIntegrityViolationException("race",new SQLException("duplicate","23000",2627));
        when(departments.saveAndFlush(any())).thenThrow(duplicate);
        // When: Gọi service.create với dữ liệu đã chuẩn bị.
        assertThrows(DepartmentFieldException.class, () -> service.create(new DepartmentRequest("Legal",null)));
        // Then: SQL 2627 thành lỗi trùng tên; lỗi khóa ngoại 547 phải giữ exception.
        var fk=new DataIntegrityViolationException("fk",new SQLException("fk","23000",547));
        doThrow(fk).when(departments).saveAndFlush(any());
        assertEquals(fk,assertThrows(DataIntegrityViolationException.class, () -> service.create(new DepartmentRequest("Legal",null))));
    }

    @Test
    @DisplayName("Phòng không tồn tại trả not found; danh sách manager loại Candidate")
    void missingDepartmentReturnsNotFoundAndManagerChoicesExcludeCandidates() {
        // Given: Phòng không tồn tại trả not found; danh sách manager loại Candidate.
        // When: Gọi service.findForEdit với dữ liệu đã chuẩn bị.
        assertThrows(ResourceNotFoundException.class, () -> service.findForEdit(77));
        // Then: Phòng không tồn tại trả not found; danh sách manager loại Candidate.
        when(users.findAllByDepartmentDepartmentIdAndAccountStatusOrderByFullNameAscUserIdAsc(7,"Active"))
            .thenReturn(List.of(member(9,"HR","Active",department),member(10,"Candidate","Active",department)));
        var choices=service.managerChoices(7);
        assertEquals(List.of(9),choices.stream().map(item->item.id()).toList());
        // Ngoài ID, dropdown phải có đúng tên hiển thị và username của manager.
        assertEquals("Member 9",choices.getFirst().fullName());
        assertEquals("member9",choices.getFirst().username());
    }
    private User member(int id,String role,String status,Department own) {
        return User.builder().userId(id).fullName("Member "+id).username("member"+id).department(own)
                .role(Role.builder().roleName(role).build()).accountStatus(status).build();
    }
}
