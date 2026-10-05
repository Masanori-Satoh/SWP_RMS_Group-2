package com.group2.rms.service;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.user.dto.DepartmentRequest;
import com.group2.rms.user.entity.*;
import com.group2.rms.user.exception.DepartmentFieldException;
import com.group2.rms.user.repository.*;
import com.group2.rms.user.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class DepartmentServiceTests {
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final DepartmentService service = new DepartmentService(departments, users);
    private final Department department = Department.builder().departmentId(7).departmentName("Sản phẩm").build();

    @Test void createTrimsNameDefaultsActiveAndHasNoManager() {
        when(departments.saveAndFlush(any())).thenAnswer(call -> { Department d=call.getArgument(0); d.setDepartmentId(8); return d; });
        assertEquals(8, service.create(new DepartmentRequest("  Thiết kế  ", null)));
        verify(departments).saveAndFlush(argThat(d -> d.getDepartmentName().equals("Thiết kế") && d.getDepartmentStatus().equals("Active") && d.getManager()==null));
        verifyNoInteractions(users);
    }
    @Test void duplicateIncludesInactiveAndDoesNotPersist() {
        when(departments.existsByDepartmentNameIgnoreCase("Sản phẩm")).thenReturn(true);
        var ex=assertThrows(DepartmentFieldException.class, () -> service.create(new DepartmentRequest("Sản phẩm", null)));
        assertEquals("departmentName", ex.getField()); verify(departments, never()).saveAndFlush(any());
    }
    @Test void invalidLengthAndManagerAtCreateAreRejected() {
        assertThrows(DepartmentFieldException.class, () -> service.create(new DepartmentRequest(" ", null)));
        assertThrows(DepartmentFieldException.class, () -> service.create(new DepartmentRequest("x".repeat(101), null)));
        assertEquals("managerId", assertThrows(DepartmentFieldException.class, () -> service.create(new DepartmentRequest("Legal", 9))).getField());
        verify(departments, never()).saveAndFlush(any());
    }
    @Test void updateOwnNameAndEligibleManagerPreservesInactiveStatus() {
        department.setDepartmentStatus("Inactive");
        var manager=member(9,"HR","Active",department);
        when(departments.findById(7)).thenReturn(Optional.of(department)); when(users.findById(9)).thenReturn(Optional.of(manager));
        service.update(7,new DepartmentRequest("Sản phẩm",9));
        assertSame(manager,department.getManager()); assertEquals("Inactive",department.getDepartmentStatus());
        verify(departments).existsByDepartmentNameIgnoreCaseAndDepartmentIdNot("Sản phẩm",7);
    }
    @Test void rejectsForeignInactiveCandidateAndMissingManagers() {
        when(departments.findById(7)).thenReturn(Optional.of(department));
        for (User invalid:List.of(member(9,"HR","Active",Department.builder().departmentId(8).build()),
                member(9,"HR","Inactive",department), member(9,"Candidate","Active",department))) {
            when(users.findById(9)).thenReturn(Optional.of(invalid));
            assertEquals("managerId",assertThrows(DepartmentFieldException.class, () -> service.update(7,new DepartmentRequest("Changed",9))).getField());
            assertEquals("Sản phẩm",department.getDepartmentName()); assertNull(department.getManager());
        }
        when(users.findById(9)).thenReturn(Optional.empty());
        assertThrows(DepartmentFieldException.class, () -> service.update(7,new DepartmentRequest("Changed",9)));
        verify(departments,never()).saveAndFlush(any());
    }
    @Test void managerCanBeExplicitlyCleared() {
        department.setManager(member(9,"HR","Active",department)); when(departments.findById(7)).thenReturn(Optional.of(department));
        service.update(7,new DepartmentRequest("Sản phẩm",null)); assertNull(department.getManager());
    }
    @Test void deactivationAndActivationPreserveIdentityAndManagerAndAreIdempotent() {
        var manager=member(9,"HR","Active",department); department.setManager(manager);
        when(departments.findById(7)).thenReturn(Optional.of(department));
        service.deactivate(7); service.deactivate(7); assertEquals("Inactive",department.getDepartmentStatus());
        service.activate(7); service.activate(7); assertEquals("Active",department.getDepartmentStatus());
        assertSame(manager,department.getManager()); assertEquals("Active",manager.getAccountStatus()); assertEquals("Sản phẩm",department.getDepartmentName());
        verify(departments,times(2)).saveAndFlush(department); verifyNoInteractions(users);
        verify(departments,never()).delete(any(Department.class)); verify(departments,never()).deleteById(any());
    }
    @Test void onlySqlDuplicateErrorsBecomeFieldErrorsOtherFailuresEscape() {
        var duplicate=new DataIntegrityViolationException("race",new SQLException("duplicate","23000",2627));
        when(departments.saveAndFlush(any())).thenThrow(duplicate);
        assertThrows(DepartmentFieldException.class, () -> service.create(new DepartmentRequest("Legal",null)));
        var fk=new DataIntegrityViolationException("fk",new SQLException("fk","23000",547));
        doThrow(fk).when(departments).saveAndFlush(any());
        assertSame(fk,assertThrows(DataIntegrityViolationException.class, () -> service.create(new DepartmentRequest("Legal",null))));
    }
    @Test void missingDepartmentReturnsNotFoundAndManagerChoicesExcludeCandidates() {
        assertThrows(ResourceNotFoundException.class, () -> service.findForEdit(77));
        when(users.findAllByDepartmentDepartmentIdAndAccountStatusOrderByFullNameAscUserIdAsc(7,"Active"))
                .thenReturn(List.of(member(9,"HR","Active",department),member(10,"Candidate","Active",department)));
        assertEquals(List.of(9),service.managerChoices(7).stream().map(item->item.id()).toList());
    }
    private User member(int id,String role,String status,Department own) {
        return User.builder().userId(id).fullName("Member "+id).username("member"+id).department(own)
                .role(Role.builder().roleName(role).build()).accountStatus(status).build();
    }
}
