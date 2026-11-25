package com.group2.rms.candidate.domain.rule;

import com.group2.rms.candidate.dto.ApplicationProgressFacts;
import com.group2.rms.candidate.service.ApplicationProgressResolver;
import com.group2.rms.candidate.dto.ApplicationProgressResponse;
import com.group2.rms.candidate.dto.ApplicationScope;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.ApplicationProgressStep;
import com.group2.rms.candidate.validator.ApplicationAccess;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Phân quyền trên đơn ứng tuyển. {@link RequisitionAccess} là bản thật để dùng đúng định nghĩa
 * "HM quản lý phòng ban" của cả hệ thống; chỉ phần đọc DB là mock.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationAccessTests {

    private static final int APPLICATION_ID = 298;

    @Mock UserRepository userRepository;

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
        access = new ApplicationAccess(new RequisitionAccess(userRepository));
    }

    @Test
    @DisplayName("HR và Director được xem mọi application; Admin bị chặn")
    void hrAndDirectorSeeEveryApplicationButAdminDoesNot() {
        // Given: HR và Director được xem mọi application.
        Application application = application(engineering, "Applied");
        for (User user : List.of(hr, director)) {
            // When: Gọi access.scopeOf với dữ liệu đã chuẩn bị.
            // Then: HR và Director được xem mọi application.
            assertEquals(ApplicationScope.ALL, access.scopeOf(user));
            assertEquals(true, access.canView(user, application));
        }
        assertEquals(false, access.canView(admin, application));
        assertThrows(AccessDeniedException.class, () -> access.scopeOf(admin));
    }

    @Test
    @DisplayName("Scope HM theo user ID và phòng ban hiện tại, không yêu cầu hồ sơ đã chuyển (Q1.4)")
    void hiringManagerScopeIsManagedOrOwnDepartment() {
        // Given: Scope HM theo user ID và phòng ban hiện tại (Q1.4: không còn cờ "chỉ đơn HR đã chuyển").
        // When: Gọi access.scopeOf với dữ liệu đã chuẩn bị.
        // Then: Scope chỉ giới hạn phòng ban.
        assertEquals(new ApplicationScope(false, 5, null), access.scopeOf(engineeringHead));
        assertEquals(new ApplicationScope(false, 6, 10), access.scopeOf(engineeringMember));
    }

    @Test
    @DisplayName("HM quản lý/cùng phòng được xem hồ sơ HR đã chuyển; HM phòng khác bị chặn")
    void hiringManagerSeesForwardedApplicationsOfOwnDepartmentOnly() {
        // Given: HM quản lý/cùng phòng được xem hồ sơ HR đã chuyển; HM phòng khác bị chặn.
        Application application = application(engineering, "HR_Passed");
        // When: Gọi access.canView với dữ liệu đã chuẩn bị.
        // Then: HM quản lý/cùng phòng được xem hồ sơ HR đã chuyển; HM phòng khác bị chặn.
        assertEquals(true, access.canView(engineeringHead, application));
        assertEquals(true, access.canView(engineeringMember, application));
        assertEquals(false, access.canView(salesHead, application));
    }

    @Test
    @DisplayName("Q1.4: HM xem được hồ sơ phòng mình HR chưa chuyển (không duyệt); HM phòng khác vẫn bị chặn")
    void hiringManagerSeesApplicationNotYetForwardedOfOwnDepartment() {
        for (String status : List.of("Applied")) {
            // Given: Hồ sơ phòng Engineering mới nộp / AI vừa chấm, HR chưa duyệt.
            Application application = application(engineering, status);
            // When: Gọi access.canView / requireView / canReview.
            // Then: HM quản lý/cùng phòng xem được nhưng không duyệt; HM phòng Sales bị chặn.
            assertEquals(true, access.canView(engineeringHead, application));
            assertEquals(true, access.canView(engineeringMember, application));
            access.requireView(engineeringHead, application);
            assertEquals(false, access.canReview(engineeringHead, application));
            assertEquals(false, access.canView(salesHead, application));
            assertThrows(AccessDeniedException.class, () -> access.requireView(salesHead, application));
        }
    }

    @Test
    @DisplayName("Interviewer không được xem application tuyển dụng nội bộ")
    void rolesOutsideRecruitmentSeeNothing() {
        // Given: Interviewer không được xem application tuyển dụng nội bộ.
        // When: Gọi access.canView với dữ liệu đã chuẩn bị.
        // Then: Interviewer không được xem application tuyển dụng nội bộ.
        assertEquals(false, access.canView(interviewer, application(engineering, "Applied")));
    }

    @Test
    @DisplayName("HR review/rescreen trước khi chuyển; HM bị chặn ở trạng thái Applied")
    void hrReviewsAndRescreensBeforeForwarding_Applied() {
        // Given: HR review/rescreen trước khi chuyển; HM bị chặn ở trạng thái Applied.
        Application application = application(engineering, "Applied");
        // When: Gọi access.canReview với dữ liệu đã chuẩn bị.
        // Then: HR review/rescreen trước khi chuyển; HM bị chặn ở trạng thái Applied.
        assertEquals(true, access.canReview(hr, application));
        assertEquals(false, access.canReview(engineeringHead, application));
    }

    @Test
    @DisplayName("Chỉ HM đúng phòng được review HR_Passed; HR không review/rescreen lại bước này")
    void hiringManagerReviewsForwardedApplicationOfOwnDepartmentOnly() {
        // Given: Chỉ HM đúng phòng được review HR_Passed; HR không review/rescreen lại bước này.
        Application application = application(engineering, "HR_Passed");
        // When: Gọi access.canReview với dữ liệu đã chuẩn bị.
        // Then: Chỉ HM đúng phòng được review HR_Passed; HR không review/rescreen lại bước này.
        assertEquals(true, access.canReview(engineeringHead, application));
        assertEquals(true, access.canReview(engineeringMember, application));
        assertEquals(false, access.canReview(salesHead, application));
        assertEquals(false, access.canReview(hr, application));
    }

    @Test
    @DisplayName("Director/Admin không được review hồ sơ trạng thái Applied")
    void directorAndAdminNeverReview_Applied() {
        // Given: Director/Admin không được review hồ sơ trạng thái Applied.
        Application application = application(engineering, "Applied");
        // When: Gọi access.canReview với dữ liệu đã chuẩn bị.
        // Then: Director/Admin không được review hồ sơ trạng thái Applied.
        assertEquals(false, access.canReview(director, application));
        assertEquals(false, access.canReview(admin, application));
    }

    @Test
    @DisplayName("Q2.3: Director duyệt thay HM hồ sơ HR_Passed của mọi phòng; Admin không")
    void directorReviewsHrPassedOfAnyDepartmentButAdminDoesNot() {
        for (Department department : List.of(engineering, sales)) {
            // Given: Hồ sơ HR đã chuyển, ở phòng Engineering hoặc Sales (Director không thuộc phòng nào).
            Application application = application(department, "HR_Passed");
            // When: Gọi access.canReview.
            // Then: Director được duyệt thay (không kiểm phòng ban); Admin không; Director không chấm lại AI.
            assertEquals(true, access.canReview(director, application));
            assertEquals(false, access.canReview(admin, application));
        }
    }

    @Test
    @DisplayName("Director/Admin không được review hồ sơ trạng thái HM_Passed")
    void directorAndAdminNeverReview_HM_Passed() {
        // Given: Director/Admin không được review hồ sơ trạng thái HM_Passed.
        Application application = application(engineering, "HM_Passed");
        // When: Gọi access.canReview với dữ liệu đã chuẩn bị.
        // Then: Director/Admin không được review hồ sơ trạng thái HM_Passed.
        assertEquals(false, access.canReview(director, application));
        assertEquals(false, access.canReview(admin, application));
    }

    @Test
    @DisplayName("Director/Admin không được review hồ sơ trạng thái Rejected")
    void directorAndAdminNeverReview_Rejected() {
        // Given: Director/Admin không được review hồ sơ trạng thái Rejected.
        Application application = application(engineering, "Rejected");
        // When: Gọi access.canReview với dữ liệu đã chuẩn bị.
        // Then: Director/Admin không được review hồ sơ trạng thái Rejected.
        assertEquals(false, access.canReview(director, application));
        assertEquals(false, access.canReview(admin, application));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước AWAITING_HR_REVIEW")
    void schedulingInterviewOnlyDuringInterviewStage_AWAITING_HR_REVIEW() {
        // Given: Quyền lên lịch theo bước AWAITING_HR_REVIEW.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.AWAITING_HR_REVIEW);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước AWAITING_HR_REVIEW.
        assertEquals(false, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước AWAITING_HM_REVIEW")
    void schedulingInterviewOnlyDuringInterviewStage_AWAITING_HM_REVIEW() {
        // Given: Quyền lên lịch theo bước AWAITING_HM_REVIEW.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.AWAITING_HM_REVIEW);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước AWAITING_HM_REVIEW.
        assertEquals(false, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước HM_SUGGESTED")
    void schedulingInterviewOnlyDuringInterviewStage_HM_SUGGESTED() {
        // Given: Quyền lên lịch theo bước HM_SUGGESTED.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.HM_SUGGESTED);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước HM_SUGGESTED.
        assertEquals(false, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước AWAITING_SCHEDULE")
    void schedulingInterviewOnlyDuringInterviewStage_AWAITING_SCHEDULE() {
        // Given: Quyền lên lịch theo bước AWAITING_SCHEDULE.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.AWAITING_SCHEDULE);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước AWAITING_SCHEDULE.
        assertEquals(true, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước INTERVIEW_SCHEDULED")
    void schedulingInterviewOnlyDuringInterviewStage_INTERVIEW_SCHEDULED() {
        // Given: Quyền lên lịch theo bước INTERVIEW_SCHEDULED.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.INTERVIEW_SCHEDULED);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước INTERVIEW_SCHEDULED.
        assertEquals(true, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước AWAITING_EVALUATIONS")
    void schedulingInterviewOnlyDuringInterviewStage_AWAITING_EVALUATIONS() {
        // Given: Quyền lên lịch theo bước AWAITING_EVALUATIONS.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.AWAITING_EVALUATIONS);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước AWAITING_EVALUATIONS.
        assertEquals(true, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước AWAITING_DECISION")
    void schedulingInterviewOnlyDuringInterviewStage_AWAITING_DECISION() {
        // Given: Quyền lên lịch theo bước AWAITING_DECISION.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.AWAITING_DECISION);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước AWAITING_DECISION.
        assertEquals(true, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước PASSED_AWAITING_OFFER")
    void schedulingInterviewOnlyDuringInterviewStage_PASSED_AWAITING_OFFER() {
        // Given: Quyền lên lịch theo bước PASSED_AWAITING_OFFER.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.PASSED_AWAITING_OFFER);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước PASSED_AWAITING_OFFER.
        assertEquals(false, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước OFFER_DRAFT")
    void schedulingInterviewOnlyDuringInterviewStage_OFFER_DRAFT() {
        // Given: Quyền lên lịch theo bước OFFER_DRAFT.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.OFFER_DRAFT);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước OFFER_DRAFT.
        assertEquals(false, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước OFFER_AWAITING_DIRECTOR")
    void schedulingInterviewOnlyDuringInterviewStage_OFFER_AWAITING_DIRECTOR() {
        // Given: Quyền lên lịch theo bước OFFER_AWAITING_DIRECTOR.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.OFFER_AWAITING_DIRECTOR);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước OFFER_AWAITING_DIRECTOR.
        assertEquals(false, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước OFFER_RETURNED")
    void schedulingInterviewOnlyDuringInterviewStage_OFFER_RETURNED() {
        // Given: Quyền lên lịch theo bước OFFER_RETURNED.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.OFFER_RETURNED);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước OFFER_RETURNED.
        assertEquals(false, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước OFFER_AWAITING_SEND")
    void schedulingInterviewOnlyDuringInterviewStage_OFFER_AWAITING_SEND() {
        // Given: Quyền lên lịch theo bước OFFER_AWAITING_SEND.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.OFFER_AWAITING_SEND);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước OFFER_AWAITING_SEND.
        assertEquals(false, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước OFFER_SENT")
    void schedulingInterviewOnlyDuringInterviewStage_OFFER_SENT() {
        // Given: Quyền lên lịch theo bước OFFER_SENT.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.OFFER_SENT);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước OFFER_SENT.
        assertEquals(false, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước HIRED_DONE")
    void schedulingInterviewOnlyDuringInterviewStage_HIRED_DONE() {
        // Given: Quyền lên lịch theo bước HIRED_DONE.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.HIRED_DONE);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước HIRED_DONE.
        assertEquals(false, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước WITHDRAWN")
    void schedulingInterviewOnlyDuringInterviewStage_WITHDRAWN() {
        // Given: Quyền lên lịch theo bước WITHDRAWN.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.WITHDRAWN);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước WITHDRAWN.
        assertEquals(false, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước OFFER_DECLINED")
    void schedulingInterviewOnlyDuringInterviewStage_OFFER_DECLINED() {
        // Given: Quyền lên lịch theo bước OFFER_DECLINED.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.OFFER_DECLINED);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước OFFER_DECLINED.
        assertEquals(false, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước INTERVIEW_FAILED")
    void schedulingInterviewOnlyDuringInterviewStage_INTERVIEW_FAILED() {
        // Given: Quyền lên lịch theo bước INTERVIEW_FAILED.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.INTERVIEW_FAILED);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước INTERVIEW_FAILED.
        assertEquals(false, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước CV_REJECTED")
    void schedulingInterviewOnlyDuringInterviewStage_CV_REJECTED() {
        // Given: Quyền lên lịch theo bước CV_REJECTED.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.CV_REJECTED);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước CV_REJECTED.
        assertEquals(false, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền lên lịch theo bước POSTING_CLOSED")
    void schedulingInterviewOnlyDuringInterviewStage_POSTING_CLOSED() {
        // Given: Quyền lên lịch theo bước POSTING_CLOSED.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.POSTING_CLOSED);
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: Quyền lên lịch theo bước POSTING_CLOSED.
        assertEquals(false, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canScheduleInterview(admin, progress));
        assertEquals(false, access.canScheduleInterview(director, progress));
        assertEquals(false, access.canScheduleInterview(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước AWAITING_HR_REVIEW")
    void creatingOfferOnlyAfterPassedInterview_AWAITING_HR_REVIEW() {
        // Given: Quyền tạo offer theo bước AWAITING_HR_REVIEW.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.AWAITING_HR_REVIEW);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước AWAITING_HR_REVIEW.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước AWAITING_HM_REVIEW")
    void creatingOfferOnlyAfterPassedInterview_AWAITING_HM_REVIEW() {
        // Given: Quyền tạo offer theo bước AWAITING_HM_REVIEW.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.AWAITING_HM_REVIEW);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước AWAITING_HM_REVIEW.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước HM_SUGGESTED")
    void creatingOfferOnlyAfterPassedInterview_HM_SUGGESTED() {
        // Given: Quyền tạo offer theo bước HM_SUGGESTED.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.HM_SUGGESTED);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước HM_SUGGESTED.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước AWAITING_SCHEDULE")
    void creatingOfferOnlyAfterPassedInterview_AWAITING_SCHEDULE() {
        // Given: Quyền tạo offer theo bước AWAITING_SCHEDULE.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.AWAITING_SCHEDULE);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước AWAITING_SCHEDULE.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước INTERVIEW_SCHEDULED")
    void creatingOfferOnlyAfterPassedInterview_INTERVIEW_SCHEDULED() {
        // Given: Quyền tạo offer theo bước INTERVIEW_SCHEDULED.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.INTERVIEW_SCHEDULED);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước INTERVIEW_SCHEDULED.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước AWAITING_EVALUATIONS")
    void creatingOfferOnlyAfterPassedInterview_AWAITING_EVALUATIONS() {
        // Given: Quyền tạo offer theo bước AWAITING_EVALUATIONS.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.AWAITING_EVALUATIONS);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước AWAITING_EVALUATIONS.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước AWAITING_DECISION")
    void creatingOfferOnlyAfterPassedInterview_AWAITING_DECISION() {
        // Given: Quyền tạo offer theo bước AWAITING_DECISION.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.AWAITING_DECISION);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước AWAITING_DECISION.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước PASSED_AWAITING_OFFER")
    void creatingOfferOnlyAfterPassedInterview_PASSED_AWAITING_OFFER() {
        // Given: Quyền tạo offer theo bước PASSED_AWAITING_OFFER.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.PASSED_AWAITING_OFFER);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước PASSED_AWAITING_OFFER.
        assertEquals(true, access.canCreateOffer(hr, progress));
        assertEquals(true, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước OFFER_DRAFT")
    void creatingOfferOnlyAfterPassedInterview_OFFER_DRAFT() {
        // Given: Quyền tạo offer theo bước OFFER_DRAFT.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.OFFER_DRAFT);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước OFFER_DRAFT.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước OFFER_AWAITING_DIRECTOR")
    void creatingOfferOnlyAfterPassedInterview_OFFER_AWAITING_DIRECTOR() {
        // Given: Quyền tạo offer theo bước OFFER_AWAITING_DIRECTOR.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.OFFER_AWAITING_DIRECTOR);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước OFFER_AWAITING_DIRECTOR.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước OFFER_RETURNED")
    void creatingOfferOnlyAfterPassedInterview_OFFER_RETURNED() {
        // Given: Quyền tạo offer theo bước OFFER_RETURNED.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.OFFER_RETURNED);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước OFFER_RETURNED.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước OFFER_AWAITING_SEND")
    void creatingOfferOnlyAfterPassedInterview_OFFER_AWAITING_SEND() {
        // Given: Quyền tạo offer theo bước OFFER_AWAITING_SEND.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.OFFER_AWAITING_SEND);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước OFFER_AWAITING_SEND.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước OFFER_SENT")
    void creatingOfferOnlyAfterPassedInterview_OFFER_SENT() {
        // Given: Quyền tạo offer theo bước OFFER_SENT.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.OFFER_SENT);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước OFFER_SENT.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước HIRED_DONE")
    void creatingOfferOnlyAfterPassedInterview_HIRED_DONE() {
        // Given: Quyền tạo offer theo bước HIRED_DONE.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.HIRED_DONE);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước HIRED_DONE.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước WITHDRAWN")
    void creatingOfferOnlyAfterPassedInterview_WITHDRAWN() {
        // Given: Quyền tạo offer theo bước WITHDRAWN.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.WITHDRAWN);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước WITHDRAWN.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước OFFER_DECLINED")
    void creatingOfferOnlyAfterPassedInterview_OFFER_DECLINED() {
        // Given: Quyền tạo offer theo bước OFFER_DECLINED.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.OFFER_DECLINED);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước OFFER_DECLINED.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước INTERVIEW_FAILED")
    void creatingOfferOnlyAfterPassedInterview_INTERVIEW_FAILED() {
        // Given: Quyền tạo offer theo bước INTERVIEW_FAILED.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.INTERVIEW_FAILED);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước INTERVIEW_FAILED.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước CV_REJECTED")
    void creatingOfferOnlyAfterPassedInterview_CV_REJECTED() {
        // Given: Quyền tạo offer theo bước CV_REJECTED.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.CV_REJECTED);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước CV_REJECTED.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    @Test
    @DisplayName("Quyền tạo offer theo bước POSTING_CLOSED")
    void creatingOfferOnlyAfterPassedInterview_POSTING_CLOSED() {
        // Given: Quyền tạo offer theo bước POSTING_CLOSED.
        ApplicationProgressResponse progress = progress(ApplicationProgressStep.POSTING_CLOSED);
        // When: Gọi access.canCreateOffer với dữ liệu đã chuẩn bị.
        // Then: Quyền tạo offer theo bước POSTING_CLOSED.
        assertEquals(false, access.canCreateOffer(hr, progress));
        assertEquals(false, access.canCreateOffer(director, progress));
        assertEquals(false, access.canCreateOffer(admin, progress));
        assertEquals(false, access.canCreateOffer(engineeringHead, progress));
    }

    /** Nghiệm thu PR 1 dòng 4: hồ sơ HM_Passed chưa có lịch có "Lên lịch", không có "Tạo offer". */
    @Test
    @DisplayName("HM_Passed chưa có lịch: HR được lên lịch, chưa được tạo offer")
    void hiringManagerPassedWithoutScheduleOffersSchedulingOnly() {
        // Given: HM_Passed chưa có lịch.
        ApplicationProgressResponse progress = ApplicationProgressResolver.resolve(
            new ApplicationProgressFacts(APPLICATION_ID, "HM_Passed", null, null, null, 0, 0, null, null),
            LocalDateTime.now());
        // When: Gọi access.canScheduleInterview với dữ liệu đã chuẩn bị.
        // Then: HR được lên lịch, chưa được tạo offer.
        assertEquals(true, access.canScheduleInterview(hr, progress));
        assertEquals(false, access.canCreateOffer(hr, progress));
    }

    @Test
    @DisplayName("HR/HM/Director là reviewer (Director duyệt thay bước HM); Admin/Interviewer không")
    void hrHiringManagersAndDirectorAreReviewers() {
        // Given: Các vai trò nội bộ.
        // When: Gọi access.isReviewer / isDirector.
        // Then: HR, HM, Director là reviewer; Admin, Interviewer không.
        assertEquals(true, access.isReviewer(hr));
        assertEquals(true, access.isReviewer(engineeringHead));
        assertEquals(true, access.isReviewer(director));
        assertEquals(true, access.isDirector(director));
        assertEquals(false, access.isDirector(engineeringHead));
        assertEquals(false, access.isReviewer(admin));
        assertEquals(false, access.isReviewer(interviewer));
        assertEquals(true, access.isHr(hr));
        assertEquals(false, access.isHr(engineeringHead));
    }

    @Test
    @DisplayName("HR/HM có link tin nội bộ; Director và Admin không có link này")
    void hrAdminAndHiringManagerGetLinkToInternalJobPosting() {
        // Given: HR/Admin/HM có link tin nội bộ; Director không có link này.
        // When: Gọi access.canOpenJobPosting với dữ liệu đã chuẩn bị.
        // Then: HR/Admin/HM có link tin nội bộ; Director không có link này.
        assertEquals(true, access.canOpenJobPosting(hr));
        assertEquals(false, access.canOpenJobPosting(admin));
        assertEquals(false, access.canOpenJobPosting(director));
        assertEquals(true, access.canOpenJobPosting(engineeringHead));
    }

    // ------------------------------------------------------------------ khôi phục hồ sơ bị loại ở vòng CV

    @Test
    @DisplayName("Khôi phục: HR chỉ khôi phục đơn HR loại; HM/Director/Admin/Interviewer thì không")
    void hrRestoresOnlyWhatHrRejected() {
        Application application = rejected(engineering, "Published");
        assertEquals(true, access.canRestore(hr, application, "HR", false));
        assertEquals(false, access.canRestore(hr, application, "HiringManager", false));
        assertEquals(false, access.canRestore(hr, application, "Director", false));
        for (User other : List.of(engineeringHead, director, admin, interviewer)) {
            assertEquals(false, access.canRestore(other, application, "HR", false));
        }
        assertEquals("Applied", ApplicationAccess.restoreTarget("HR"));
    }

    @Test
    @DisplayName("Khôi phục: HM đúng phòng hoặc Giám đốc khôi phục đơn HM/Giám đốc loại; HM phòng khác, HR, Admin thì không")
    void hiringManagerOfTheDepartmentOrDirectorRestoresWhatHmRejected() {
        Application application = rejected(engineering, "Published");
        for (String failedBy : List.of("HiringManager", "Director")) {
            assertEquals(true, access.canRestore(engineeringHead, application, failedBy, false));
            assertEquals(true, access.canRestore(engineeringMember, application, failedBy, false));
            assertEquals(true, access.canRestore(director, application, failedBy, false));
            assertEquals(false, access.canRestore(salesHead, application, failedBy, false), "HM phòng khác");
            assertEquals(false, access.canRestore(hr, application, failedBy, false), "HR không khôi phục đơn HM loại");
            assertEquals(false, access.canRestore(admin, application, failedBy, false));
        }
        assertEquals("HR_Passed", ApplicationAccess.restoreTarget("HiringManager"));
        assertEquals("HR_Passed", ApplicationAccess.restoreTarget("Director"));
    }

    @Test
    @DisplayName("Khôi phục: tin Paused vẫn được; tin Closed, trượt phỏng vấn, talent pool đã hủy thì không")
    void restoreNeedsAnOpenPostingAndACvRoundRejection() {
        assertEquals(true, access.canRestore(hr, rejected(engineering, "Paused"), "HR", false));
        assertEquals(false, access.canRestore(hr, rejected(engineering, "Closed"), "HR", false));
        assertEquals(false, access.canRestore(hr, rejected(engineering, null), "HR", false));
        assertEquals(false, access.canRestore(hr, rejected(engineering, "Published"), null, false),
                "lượt duyệt mới nhất không phải Fail (trượt phỏng vấn)");
        assertEquals(false, access.canRestore(hr, rejected(engineering, "Published"), "HR", true),
                "đề xuất talent pool đã hủy");
        assertEquals(false, access.canRestore(hr, rejected(engineering, "Published"), "Unknown", false));
    }

    @Test
    @DisplayName("Khôi phục: chỉ đơn đang Rejected; Withdrawn, Cancelled, Hired và đơn đã khôi phục (Applied) thì không")
    void onlyRejectedApplicationsCanBeRestored() {
        for (String status : List.of("Withdrawn", "Cancelled", "Hired", "Applied", "HR_Passed", "Interviewing")) {
            Application application = rejected(engineering, "Published");
            application.setApplicationStatus(status);
            assertEquals(false, access.canRestore(hr, application, "HR", false), status);
            assertEquals(false, access.canRestore(director, application, "HiringManager", false), status);
        }
    }

    @Test
    @DisplayName("Khôi phục: restoreBlockedReason trả đúng câu báo lỗi cho từng điều kiện, null nếu thỏa")
    void restoreBlockedReasonNamesTheFailedCondition() {
        Application ok = rejected(engineering, "Published");
        assertEquals(null, access.restoreBlockedReason(ok, "HR", false));

        Application moved = rejected(engineering, "Published");
        moved.setApplicationStatus("Applied");
        assertEquals("Hồ sơ không còn ở trạng thái Không đạt. Vui lòng tải lại trang.",
                access.restoreBlockedReason(moved, "HR", false));
        assertEquals("Đề xuất ứng viên đã bị hủy nên không thể khôi phục hồ sơ.",
                access.restoreBlockedReason(ok, "HR", true));
        assertEquals("Chỉ khôi phục được hồ sơ bị loại ở vòng xem CV.", access.restoreBlockedReason(ok, null, false));
        assertEquals("Tin tuyển dụng đã đóng nên không thể khôi phục hồ sơ.",
                access.restoreBlockedReason(rejected(engineering, "Closed"), "HR", false));
    }

    private static Application rejected(Department department, String postingStatus) {
        JobRequisition requisition = JobRequisition.builder().requisitionId(7).department(department).build();
        return Application.builder()
                .applicationId(APPLICATION_ID)
                .jobPosting(JobPosting.builder().requisition(requisition).postingStatus(postingStatus).build())
                .applicationStatus("Rejected")
                .build();
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
