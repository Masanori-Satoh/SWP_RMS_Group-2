package com.group2.rms.candidate.service;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.repository.ApplicationReviewRepository;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Phân quyền trên đơn ứng tuyển. {@link RequisitionAccess} là bản thật để dùng đúng định nghĩa
 * "HM quản lý phòng ban" của cả hệ thống; chỉ phần đọc DB là mock.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationAccessTests {

    private static final int APPLICATION_ID = 298;

    @Mock UserRepository userRepository;
    @Mock ApplicationReviewRepository reviews;

    private ApplicationAccess access;

    private final User hr = user(1, "HR", null);
    private final User director = user(2, "Director", null);
    private final User admin = user(3, "System Admin", null);
    private final User interviewer = user(4, "Interviewer", null);
    private final Department engineering = Department.builder().departmentId(10).build();
    private final Department sales = Department.builder().departmentId(20).build();
    private final User engineeringHead = user(5, "Hiring Manager", null);
    private final User engineeringMember = user(6, "Hiring Manager", engineering);
    private final User salesHead = user(7, "Hiring Manager", null);

    @BeforeEach
    void setUp() {
        engineering.setManager(engineeringHead);
        sales.setManager(salesHead);
        access = new ApplicationAccess(new RequisitionAccess(userRepository), reviews);
    }

    @Test
    void hrDirectorAndAdminSeeEveryApplication() {
        Application application = application(engineering, "Applied");

        for (User user : List.of(hr, director, admin)) {
            assertEquals(ApplicationScope.ALL, access.scopeOf(user));
            assertTrue(access.canView(user, application));
        }
        verifyNoInteractions(reviews);
    }

    @Test
    void hiringManagerScopeIsManagedOrOwnDepartmentAndForwardedOnly() {
        assertEquals(new ApplicationScope(false, 5, null, true), access.scopeOf(engineeringHead));
        assertEquals(new ApplicationScope(false, 6, 10, true), access.scopeOf(engineeringMember));
    }

    @Test
    void hiringManagerSeesOnlyForwardedApplicationsOfOwnDepartment() {
        Application application = application(engineering, "HR_Passed");
        when(reviews.existsByApplication_ApplicationIdAndReviewerRoleAndDecision(APPLICATION_ID, "HR", "Pass"))
                .thenReturn(true);

        assertTrue(access.canView(engineeringHead, application));
        assertTrue(access.canView(engineeringMember, application));
        assertFalse(access.canView(salesHead, application));
    }

    @Test
    void hiringManagerCannotSeeApplicationNotYetForwarded() {
        Application application = application(engineering, "AI_Screened");
        when(reviews.existsByApplication_ApplicationIdAndReviewerRoleAndDecision(anyInt(), anyString(), anyString()))
                .thenReturn(false);

        assertFalse(access.canView(engineeringHead, application));
        assertThrows(AccessDeniedException.class, () -> access.requireView(engineeringHead, application));
    }

    @Test
    void rolesOutsideRecruitmentSeeNothing() {
        assertFalse(access.canView(interviewer, application(engineering, "Applied")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Applied", "AI_Screened"})
    void hrReviewsAndRescreensBeforeForwarding(String status) {
        Application application = application(engineering, status);

        assertTrue(access.canReview(hr, application));
        assertTrue(access.canRescreen(hr, application));
        assertFalse(access.canReview(engineeringHead, application));
        assertFalse(access.canRescreen(engineeringHead, application));
    }

    @Test
    void hiringManagerReviewsForwardedApplicationOfOwnDepartmentOnly() {
        Application application = application(engineering, "HR_Passed");

        assertTrue(access.canReview(engineeringHead, application));
        assertTrue(access.canReview(engineeringMember, application));
        assertFalse(access.canReview(salesHead, application));
        assertFalse(access.canReview(hr, application));
        assertFalse(access.canRescreen(hr, application));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Applied", "AI_Screened", "HR_Passed", "HM_Passed", "Rejected"})
    void directorAndAdminNeverReview(String status) {
        Application application = application(engineering, status);

        assertFalse(access.canReview(director, application));
        assertFalse(access.canReview(admin, application));
    }

    @ParameterizedTest
    @ValueSource(strings = {"HM_Passed", "Interviewing"})
    void nextStepsFollowTeammatesRouteRoles(String status) {
        assertTrue(access.canScheduleInterview(hr, status));
        assertTrue(access.canScheduleInterview(admin, status));
        assertFalse(access.canScheduleInterview(director, status));
        assertFalse(access.canScheduleInterview(engineeringHead, status));

        assertTrue(access.canCreateOffer(hr, status));
        assertTrue(access.canCreateOffer(director, status));
        assertTrue(access.canCreateOffer(admin, status));
        assertFalse(access.canCreateOffer(engineeringHead, status));
    }

    @Test
    void nextStepsHiddenBeforeHiringManagerPasses() {
        assertFalse(access.canScheduleInterview(hr, "HR_Passed"));
        assertFalse(access.canCreateOffer(hr, "HR_Passed"));
    }

    @Test
    void onlyHrAndAdminGetLinkToInternalJobPosting() {
        assertTrue(access.canOpenJobPosting(hr));
        assertTrue(access.canOpenJobPosting(admin));
        assertFalse(access.canOpenJobPosting(director));
        assertFalse(access.canOpenJobPosting(engineeringHead));
    }

    private static User user(int id, String roleName, Department department) {
        return User.builder()
                .userId(id)
                .role(Role.builder().roleName(roleName).build())
                .department(department)
                .build();
    }

    private static Application application(Department department, String status) {
        JobRequisition requisition = JobRequisition.builder().requisitionId(7).department(department).build();
        return Application.builder()
                .applicationId(APPLICATION_ID)
                .jobPosting(JobPosting.builder().requisition(requisition).build())
                .applicationStatus(status)
                .build();
    }
}
