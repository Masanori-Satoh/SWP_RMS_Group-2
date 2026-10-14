package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.ApplicationProgressResponse;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
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
    @Mock com.group2.rms.candidate.repository.ApplicationRepository applications;

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
        access = new ApplicationAccess(new RequisitionAccess(userRepository), reviews, applications);
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

    /** Lên lịch: HR, Admin (quyền URL bên interview), chỉ ở giai đoạn phỏng vấn; sang offer thì ẩn. */
    @ParameterizedTest
    @EnumSource(ApplicationProgressStep.class)
    void schedulingInterviewOnlyDuringInterviewStage(ApplicationProgressStep step) {
        ApplicationProgressResponse progress = progress(step);
        boolean interviewStage = step.stage() == ApplicationStage.INTERVIEW;

        assertEquals(interviewStage, access.canScheduleInterview(hr, progress));
        assertEquals(interviewStage, access.canScheduleInterview(admin, progress));
        assertFalse(access.canScheduleInterview(director, progress));
        assertFalse(access.canScheduleInterview(engineeringHead, progress));
    }

    /** Tạo offer: HR, Director, Admin (quyền URL bên offer), chỉ khi đã đạt phỏng vấn và chưa có offer. */
    @ParameterizedTest
    @EnumSource(ApplicationProgressStep.class)
    void creatingOfferOnlyAfterPassedInterview(ApplicationProgressStep step) {
        ApplicationProgressResponse progress = progress(step);
        boolean passed = step == ApplicationProgressStep.PASSED_AWAITING_OFFER;

        assertEquals(passed, access.canCreateOffer(hr, progress));
        assertEquals(passed, access.canCreateOffer(director, progress));
        assertEquals(passed, access.canCreateOffer(admin, progress));
        assertFalse(access.canCreateOffer(engineeringHead, progress));
    }

    /** Nghiệm thu PR 1 dòng 4: hồ sơ HM_Passed chưa có lịch có "Lên lịch", không có "Tạo offer". */
    @Test
    void hiringManagerPassedWithoutScheduleOffersSchedulingOnly() {
        ApplicationProgressResponse progress = ApplicationProgressResolver.resolve(
                new ApplicationProgressFacts(APPLICATION_ID, "HM_Passed", false, null, null, 0, 0, null, null),
                LocalDateTime.now());

        assertTrue(access.canScheduleInterview(hr, progress));
        assertFalse(access.canCreateOffer(hr, progress));
    }

    @Test
    void onlyHrAndHiringManagersAreReviewers() {
        assertTrue(access.isReviewer(hr));
        assertTrue(access.isReviewer(engineeringHead));
        assertFalse(access.isReviewer(director));
        assertFalse(access.isReviewer(admin));
        assertFalse(access.isReviewer(interviewer));
        assertTrue(access.isHr(hr));
        assertFalse(access.isHr(engineeringHead));
    }

    @Test
    void hrAdminAndHiringManagerGetLinkToInternalJobPosting() {
        assertTrue(access.canOpenJobPosting(hr));
        assertTrue(access.canOpenJobPosting(admin));
        assertFalse(access.canOpenJobPosting(director));
        assertTrue(access.canOpenJobPosting(engineeringHead));
    }

    private static User user(int id, String roleName, Department department) {
        return User.builder()
                .userId(id)
                .role(Role.builder().roleName(roleName).build())
                .department(department)
                .build();
    }

    private static ApplicationProgressResponse progress(ApplicationProgressStep step) {
        return new ApplicationProgressResponse(step.stage(), step, step.label(), step.owner(), step.tone());
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
