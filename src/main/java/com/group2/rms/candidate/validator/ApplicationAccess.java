package com.group2.rms.candidate.validator;

import com.group2.rms.candidate.dto.ApplicationProgressResponse;
import com.group2.rms.candidate.dto.ApplicationScope;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.ApplicationProgressStep;
import com.group2.rms.candidate.entity.ApplicationStage;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.group2.rms.requisition.service.RequisitionAccess.*;

/**
 * Ai xem được đơn nào và làm được gì trên đơn (5.1.22, 5.1.23).
 * HR, Director xem mọi đơn; Hiring Manager xem mọi đơn phòng ban mình, kể cả
 * đơn HR chưa chuyển (Q1.4).
 * HR duyệt bước HR; Hiring Manager duyệt bước HM của phòng mình; Director duyệt thay ở bước HM (mọi phòng, khi HM
 * vắng — Q2.3), không duyệt bước HR.
 */
@Component
@RequiredArgsConstructor
public class ApplicationAccess {

    public static final Set<String> HR_REVIEW_STATUSES = Set.of("Applied");
    public static final String HM_REVIEW_STATUS = "HR_Passed";
    /** Tin còn mở thì mới khôi phục được đơn đã loại; tin Closed thì không. */
    private static final Set<String> RESTORABLE_POSTING_STATUSES = Set.of("Published", "Paused");

    private static final Set<String> VIEW_ALL_ROLES = Set.of(ROLE_HR, ROLE_DIRECTOR);

    private final RequisitionAccess requisitionAccess;

    /**
     * Người đang đăng nhập; tài khoản khóa hoặc vai trò ngoài nhóm tuyển dụng thì
     * ném {@link AccessDeniedException}.
     */
    public User actor() {
        return requisitionAccess.actor();
    }

    public ApplicationScope scopeOf(User user) {
        if (isHiringManager(user)) {
            Integer departmentId = user.getDepartment() == null ? null : user.getDepartment().getDepartmentId();
            return ApplicationScope.hiringManager(user.getUserId(), departmentId);
        }
        if (VIEW_ALL_ROLES.contains(role(user))) {
            return ApplicationScope.ALL;
        }
        throw new AccessDeniedException("Vai trò không có quyền xem hồ sơ ứng tuyển.");
    }

    public boolean canView(User user, Application application) {
        //check if can view 
        if (VIEW_ALL_ROLES.contains(role(user))) {
            return true;
        }
        // dunglt bổ sung: Q1.4 HM xem mọi đơn phòng mình; điều kiện "HR đã chuyển / nguồn talent pool" không còn cần
        return isHiringManager(user) && inManagedDepartment(user, application);
    }

    public void requireView(User user, Application application) {
        if (!canView(user, application)) {
            throw new AccessDeniedException("Bạn không có quyền xem hồ sơ này.");
        }
    }

    /**
     * HR duyệt đơn chưa qua vòng nhân sự; HM duyệt đơn HR đã chuyển, đúng phòng
     * ban; Director duyệt thay HM đơn HR đã chuyển, không kiểm phòng ban.
     */
    public boolean canReview(User user, Application application) {
        String status = application.getApplicationStatus();
        if (ROLE_HR.equals(role(user))) {
            return HR_REVIEW_STATUSES.contains(status);
        }
        if (isDirector(user)) {
            return HM_REVIEW_STATUS.equals(status);
        }
        return isHiringManager(user) && HM_REVIEW_STATUS.equals(status) && inManagedDepartment(user, application);
    }

    /**
     * Khôi phục đơn bị loại ở vòng CV (spec 08/11), một nguồn cho service và trang chi tiết. Đủ khi: đơn đang
     * {@code Rejected}; lượt duyệt mới nhất là {@code Fail} do {@code lastFailRole} ({@code null} nếu không phải
     * Fail, ví dụ trượt phỏng vấn); tin còn mở (Published, Paused); không phải đề xuất talent pool đã hủy; và
     * người bấm đúng vai trò: HR khôi phục đơn HR loại; HM đúng phòng hoặc Giám đốc khôi phục đơn HM/Giám đốc loại.
     */
    public boolean canRestore(User user, Application application, String lastFailRole, boolean proposalCancelled) {
        if (restoreBlockedReason(application, lastFailRole, proposalCancelled) != null) {
            return false;
        }
        return switch (lastFailRole) {
            case "HR" -> isHr(user);
            case "HiringManager", "Director" -> isDirector(user)
                    || (isHiringManager(user) && inManagedDepartment(user, application));
            default -> false;
        };
    }

    /**
     * Phần điều kiện khôi phục không phụ thuộc người bấm (1 đến 4 của spec); {@code null} nếu thỏa, ngược lại là
     * câu báo lỗi cho người dùng. Tách riêng để service phân biệt "đơn không khôi phục được" (lỗi hiện trên trang)
     * với "sai vai trò" (403).
     */
    public String restoreBlockedReason(Application application, String lastFailRole, boolean proposalCancelled) {
        if (!ApplicationStatusService.REJECTED.equals(application.getApplicationStatus())) {
            return "Hồ sơ không còn ở trạng thái Không đạt. Vui lòng tải lại trang.";
        }
        if (proposalCancelled) {
            return "Đề xuất ứng viên đã bị hủy nên không thể khôi phục hồ sơ.";
        }
        if (lastFailRole == null) {
            return "Chỉ khôi phục được hồ sơ bị loại ở vòng xem CV.";
        }
        String postingStatus = application.getJobPosting() == null ? null : application.getJobPosting().getPostingStatus();
        if (postingStatus == null || !RESTORABLE_POSTING_STATUSES.contains(postingStatus)) {
            return "Tin tuyển dụng đã đóng nên không thể khôi phục hồ sơ.";
        }
        return null;
    }

    /** Trạng thái đơn quay về: đúng bước đã loại. HR loại thì về {@code Applied}, HM/Giám đốc loại thì về {@code HR_Passed}. */
    public static String restoreTarget(String lastFailRole) {
        return "HR".equals(lastFailRole) ? "Applied" : HM_REVIEW_STATUS;
    }

    /**
     * Khớp quyền URL {@code /interviews/new} (HR) bên module interview; chỉ
     * khi hồ sơ đang ở giai đoạn
     * phỏng vấn (sang offer thì thôi).
     */
    public boolean canScheduleInterview(User user, ApplicationProgressResponse progress) {
        return ROLE_HR.equals(role(user))
                && progress.stage() == ApplicationStage.INTERVIEW;
    }

    /**
     * Khớp quyền URL {@code /offers/**} (HR, Director) bên module offer; chỉ
     * khi đã đạt phỏng vấn, chưa có offer.
     */
    public boolean canCreateOffer(User user, ApplicationProgressResponse progress) {
        return VIEW_ALL_ROLES.contains(role(user)) && progress.step() == ApplicationProgressStep.PASSED_AWAITING_OFFER;
    }

    /**
     * Khớp quyền URL {@code /internal/job-postings/**} (HR, Hiring Manager): chỉ khi đó mới
     * hiện link sang trang tin nội bộ.
     */
    public boolean canOpenJobPosting(User user) {
        return Set.of(ROLE_HR, ROLE_HIRING_MANAGER).contains(role(user));
    }

    /** HR, Hiring Manager, hoặc Director (duyệt thay bước HM): các vai trò có bước duyệt hồ sơ. */
    public boolean isReviewer(User user) {
        return isHr(user) || isHiringManager(user) || isDirector(user);
    }

    public boolean isHr(User user) {
        return ROLE_HR.equals(role(user));
    }

    public boolean isDirector(User user) {
        return ROLE_DIRECTOR.equals(role(user));
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
}
