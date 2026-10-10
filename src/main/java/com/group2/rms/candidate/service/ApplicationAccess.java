package com.group2.rms.candidate.service;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.repository.ApplicationReviewRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.group2.rms.requisition.service.RequisitionAccess.*;

/**
 * Ai xem được đơn nào và làm được gì trên đơn (5.1.22, 5.1.23).
 * HR, Director, Admin xem mọi đơn; Hiring Manager chỉ xem đơn phòng ban mình đã được HR chuyển (SRS UC 43).
 * Chỉ HR và Hiring Manager được duyệt; Director, Admin chỉ xem.
 */
@Component
@RequiredArgsConstructor
public class ApplicationAccess {

    static final Set<String> HR_REVIEW_STATUSES = Set.of("Applied", "AI_Screened");
    static final String HM_REVIEW_STATUS = "HR_Passed";
    static final Set<String> NEXT_STEP_STATUSES = Set.of("HM_Passed", "Interviewing");

    private static final Set<String> VIEW_ALL_ROLES = Set.of(ROLE_HR, ROLE_DIRECTOR, ROLE_SYSTEM_ADMIN);

    private final RequisitionAccess requisitionAccess;
    private final ApplicationReviewRepository reviews;

    /** Người đang đăng nhập; tài khoản khóa hoặc vai trò ngoài nhóm tuyển dụng thì ném {@link AccessDeniedException}. */
    public User actor() {
        return requisitionAccess.actor();
    }

    public ApplicationScope scopeOf(User user) {
        if (isHiringManager(user)) {
            Integer departmentId = user.getDepartment() == null ? null : user.getDepartment().getDepartmentId();
            return ApplicationScope.hiringManager(user.getUserId(), departmentId);
        }
        return ApplicationScope.ALL;
    }

    public boolean canView(User user, Application application) {
        if (VIEW_ALL_ROLES.contains(role(user))) {
            return true;
        }
        return isHiringManager(user) && inManagedDepartment(user, application) && forwardedByHr(application);
    }

    public void requireView(User user, Application application) {
        if (!canView(user, application)) {
            throw new AccessDeniedException("Bạn không có quyền xem hồ sơ này.");
        }
    }

    /** HR duyệt đơn chưa qua vòng nhân sự; HM duyệt đơn HR đã chuyển, đúng phòng ban. */
    public boolean canReview(User user, Application application) {
        String status = application.getApplicationStatus();
        if (ROLE_HR.equals(role(user))) {
            return HR_REVIEW_STATUSES.contains(status);
        }
        return isHiringManager(user) && HM_REVIEW_STATUS.equals(status) && inManagedDepartment(user, application);
    }

    public boolean canRescreen(User user, Application application) {
        return ROLE_HR.equals(role(user)) && HR_REVIEW_STATUSES.contains(application.getApplicationStatus());
    }

    /** Khớp quyền URL {@code /interviews/new} (HR, Admin) bên module interview. */
    public boolean canScheduleInterview(User user, String applicationStatus) {
        return Set.of(ROLE_HR, ROLE_SYSTEM_ADMIN).contains(role(user)) && NEXT_STEP_STATUSES.contains(applicationStatus);
    }

    /** Khớp quyền URL {@code /offers/**} (HR, Director, Admin) bên module offer. */
    public boolean canCreateOffer(User user, String applicationStatus) {
        return VIEW_ALL_ROLES.contains(role(user)) && NEXT_STEP_STATUSES.contains(applicationStatus);
    }

    /** Khớp quyền URL {@code /internal/job-postings/**} (HR, Admin): chỉ khi đó mới hiện link sang trang tin nội bộ. */
    public boolean canOpenJobPosting(User user) {
        return Set.of(ROLE_HR, ROLE_SYSTEM_ADMIN).contains(role(user));
    }

    /** HR hoặc Hiring Manager: hai vai trò duy nhất có bước duyệt hồ sơ. */
    public boolean isReviewer(User user) {
        return isHr(user) || isHiringManager(user);
    }

    public boolean isHr(User user) {
        return ROLE_HR.equals(role(user));
    }

    private boolean isHiringManager(User user) {
        return ROLE_HIRING_MANAGER.equals(role(user));
    }

    private String role(User user) {
        return requisitionAccess.role(user);
    }

    private boolean inManagedDepartment(User user, Application application) {
        return requisitionAccess.inManagedDepartment(user, application.getJobPosting().getRequisition());
    }

    private boolean forwardedByHr(Application application) {
        return reviews.existsByApplication_ApplicationIdAndReviewerRoleAndDecision(
                application.getApplicationId(), "HR", "Pass");
    }
}
