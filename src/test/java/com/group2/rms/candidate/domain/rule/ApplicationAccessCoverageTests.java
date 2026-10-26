package com.group2.rms.candidate.domain.rule;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ApplicationAccessCoverageTests {
    private final RequisitionAccess requisitionAccess = mock(RequisitionAccess.class);
    private final ApplicationAccess service = new ApplicationAccess(requisitionAccess);
    private final User user = User.builder().userId(7).build();
    private final JobRequisition requisition = JobRequisition.builder().build();
    private final Application application = Application.builder().applicationId(100)
            .jobPosting(JobPosting.builder().requisition(requisition).build()).build();

    @Test
    @DisplayName("AUTHZ-APP-01 | Lấy actor từ bộ kiểm tra quyền requisition")
    void actor_returnsAuthorizedUser() {
        // Given: Bộ kiểm tra requisition xác định user 7 là actor hợp lệ.
        when(requisitionAccess.actor()).thenReturn(user);
        // When: Lấy actor của module application.
        User result = service.actor();
        // Then: Trả đúng user và chỉ kiểm tra actor một lần.
        assertEquals(user, result);
        verify(requisitionAccess, times(1)).actor();
    }

    @Test
    @DisplayName("AUTHZ-APP-02 | Candidate bị từ chối xem hồ sơ nội bộ")
    void requireView_candidate_deniesAccess() {
        // Given: Người xem có vai trò Candidate.
        when(requisitionAccess.role(user)).thenReturn("Candidate");
        // When: Yêu cầu quyền xem hồ sơ nội bộ.
        assertThrows(AccessDeniedException.class, () -> service.requireView(user, application));
        // Then: Không kiểm phạm vi phòng ban khi vai trò không hợp lệ.
        verify(requisitionAccess, times(0)).inManagedDepartment(any(), any());
    }

    @Test
    @DisplayName("AUTHZ-APP-03 | HR được xem hồ sơ và không cần kiểm tra phòng ban")
    void requireView_hr_allowsAccess() {
        // Given: Người xem có vai trò HR.
        when(requisitionAccess.role(user)).thenReturn("HR");
        // When: Kiểm tra quyền xem hồ sơ.
        service.requireView(user, application);
        // Then: Cho phép xem và không gọi kiểm tra phạm vi phòng ban.
        verify(requisitionAccess, times(1)).role(user);
        verify(requisitionAccess, times(0)).inManagedDepartment(any(), any());
    }

    @Test
    @DisplayName("AUTHZ-APP-04 | HM xem hồ sơ phòng ban mình, kể cả chưa có HR Pass (Q1.4)")
    void canView_hmOwnDepartment_allowsWithoutHrPass() {
        // dunglt bổ sung: bản cũ cần nguồn talent pool; Q1.4 cho HM xem mọi hồ sơ phòng mình
        // Given: HM đúng phòng ban, hồ sơ chưa có HR Pass.
        when(requisitionAccess.role(user)).thenReturn("Hiring Manager");
        when(requisitionAccess.inManagedDepartment(user, requisition)).thenReturn(true);
        // When: Kiểm tra quyền xem hồ sơ.
        boolean result = service.canView(user, application);
        // Then: HM được xem hồ sơ đúng phạm vi.
        assertEquals(true, result);
    }

    @Test
    @DisplayName("AUTHZ-APP-05 | HM phòng khác bị từ chối xem hồ sơ")
    void canView_hmOtherDepartment_denies() {
        // Given: HM không quản lý phòng ban của hồ sơ.
        when(requisitionAccess.role(user)).thenReturn("Hiring Manager");
        when(requisitionAccess.inManagedDepartment(user, requisition)).thenReturn(false);
        // When / Then: Bị từ chối.
        assertEquals(false, service.canView(user, application));
        assertThrows(AccessDeniedException.class, () -> service.requireView(user, application));
    }
}
