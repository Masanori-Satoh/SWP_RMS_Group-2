package com.group2.rms.jobposting.service;

import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/*
 * ACCESS: luật "ai được làm gì" với tin (canX trả boolean, requireX ném 403) và hằng trạng thái tin.
 */
@Component
@RequiredArgsConstructor
public class JobPostingAccess {

    // Giá trị PostingStatus trong DB (CK_JobPosting_Status chỉ có 4 giá trị này)
    public static final String STATUS_DRAFT = "Draft";
    public static final String STATUS_PUBLISHED = "Published";
    public static final String STATUS_PAUSED = "Paused";
    public static final String STATUS_CLOSED = "Closed";

    /** Tin "đang mở" của một yêu cầu: mỗi yêu cầu chỉ được có 1 tin ở các trạng thái này. */
    public static final List<String> OPEN_STATUSES = List.of(STATUS_DRAFT, STATUS_PUBLISHED, STATUS_PAUSED);
    /** Trạng thái hiển thị theo thứ tự trên bộ lọc. */
    public static final List<String> VIEW_STATUSES =
            List.of(STATUS_DRAFT, STATUS_PUBLISHED, STATUS_PAUSED, STATUS_CLOSED);
    /** HM và Director không xem tin nháp. */
    public static final List<String> VIEW_STATUSES_WITHOUT_DRAFT =
            List.of(STATUS_PUBLISHED, STATUS_PAUSED, STATUS_CLOSED);

    private static final Set<String> EDITABLE_STATUSES = Set.of(STATUS_DRAFT, STATUS_PUBLISHED, STATUS_PAUSED);
    private static final Set<String> MANAGER_ROLES =
            Set.of(RequisitionAccess.ROLE_HR);
    private static final Set<String> READER_ROLES = Set.of(RequisitionAccess.ROLE_HR,
            RequisitionAccess.ROLE_HIRING_MANAGER, RequisitionAccess.ROLE_DIRECTOR);
    private static final Set<String> TALENT_POOL_ROLES =
            Set.of(RequisitionAccess.ROLE_HR, RequisitionAccess.ROLE_HIRING_MANAGER);

    /** Dùng lại cách đọc role và phòng ban của module Requisition (một nguồn cho cả hai module). */
    private final RequisitionAccess requisitionAccess;

    // ----- Trạng thái (hàm static: so chuỗi, không cần người dùng) -----

    /** Tin đã đóng thì không sửa được nữa ("Đăng lại" = tạo tin mới). */
    public static boolean isEditableStatus(String postingStatus) {
        return EDITABLE_STATUSES.contains(postingStatus);
    }

    /** Tin đang đăng hoặc tạm dừng sau khi đã đăng. */
    public static boolean isLive(String viewStatus) {
        return STATUS_PUBLISHED.equals(viewStatus) || STATUS_PAUSED.equals(viewStatus);
    }

    /** Tin đã từng hiện cho ứng viên (đang đăng / tạm dừng / đã đóng) nên mới có hồ sơ để thống kê. */
    public static boolean hasBeenPublished(String viewStatus) {
        return isLive(viewStatus) || STATUS_CLOSED.equals(viewStatus);
    }

    // ----- Người dùng -----

    /** Người đang đăng nhập (lấy từ phiên, không từ form). */
    public User actor() {
        return requisitionAccess.actor();
    }

    public String role(User user) {
        return requisitionAccess.role(user);
    }

    /** HR được tạo / sửa / đăng / tạm dừng / xóa tin. */
    public boolean canManage(User user) {
        return MANAGER_ROLES.contains(role(user));
    }

    /** Rule (D7): chỉ HR đóng tin, vì đóng tin còn xử lý Offer / đề xuất ứng viên (TalentPoolService#closeJob). */
    public boolean canCloseAny(User user) {
        return RequisitionAccess.ROLE_HR.equals(role(user));
    }

    /** Được vào màn tin tuyển dụng nội bộ: HR, Hiring Manager, Director (Director chỉ xem). */
    public boolean canAccessPostings(User user) {
        return READER_ROLES.contains(role(user));
    }

    public boolean isHiringManager(User user) {
        return RequisitionAccess.ROLE_HIRING_MANAGER.equals(role(user));
    }

    /** Khối "Ứng viên tiềm năng" (module talent pool) ở trang chi tiết: HR và HM. */
    public boolean canSeeTalentPool(User user) {
        return TALENT_POOL_ROLES.contains(role(user));
    }

    /** Rule (D5): HM chỉ xem tin phòng mình; HM và Director không xem tin nháp. */
    public boolean canView(User user, JobPosting posting) {
        if (!canAccessPostings(user)) {
            return false;
        }
        JobRequisition requisition = posting.getRequisition();
        if (isHiringManager(user) && !requisitionAccess.inManagedDepartment(user, requisition)) {
            return false;
        }
        return canManage(user) || !STATUS_DRAFT.equals(posting.getPostingStatus());
    }

    // ----- Cờ thao tác theo trạng thái hiển thị (viewStatus) -----

    public boolean canPublish(User user, String viewStatus) {
        return canManage(user) && STATUS_DRAFT.equals(viewStatus);
    }

    public boolean canReopen(User user, String viewStatus) {
        return canManage(user) && STATUS_PAUSED.equals(viewStatus);
    }

    public boolean canEdit(User user, String viewStatus) {
        return canManage(user) && viewStatus != null && !STATUS_CLOSED.equals(viewStatus);
    }

    public boolean canPause(User user, String viewStatus) {
        return canManage(user) && STATUS_PUBLISHED.equals(viewStatus);
    }

    public boolean canClose(User user, String viewStatus) {
        return canCloseAny(user) && isLive(viewStatus);
    }

    /** Chỉ xóa bản nháp chưa có hồ sơ ứng tuyển. */
    public boolean canDelete(User user, String viewStatus, boolean hasApplications) {
        return canManage(user) && STATUS_DRAFT.equals(viewStatus) && !hasApplications;
    }

    /** Rule (D10): đăng lại khi tin đã đóng, yêu cầu vẫn Approved và chưa có tin nào đang mở. */
    public boolean canRepost(User user, String viewStatus, JobRequisition requisition, boolean hasOpenPosting) {
        return canManage(user) && STATUS_CLOSED.equals(viewStatus) && requisition != null
                && RequisitionAccess.STATUS_APPROVED.equals(requisition.getApprovalStatus()) && !hasOpenPosting;
    }

    // ----- requireX: ném AccessDeniedException (trang 403) -----

    public void requireManage(User user) {
        if (!canManage(user)) {
            throw new AccessDeniedException("Chỉ HR hoặc Quản trị viên hệ thống mới có quyền quản lý tin tuyển dụng.");
        }
    }

    public void requireAccessPostings(User user) {
        if (!canAccessPostings(user)) {
            throw new AccessDeniedException("Bạn không có quyền xem tin tuyển dụng.");
        }
    }

    public void requireCloseAny(User user) {
        if (!canCloseAny(user)) {
            throw new AccessDeniedException("Chỉ HR được đóng đợt tuyển dụng.");
        }
    }

    public void requireView(User user, JobPosting posting) {
        requireAccessPostings(user);
        if (!canView(user, posting)) {
            throw new AccessDeniedException("Bạn không có quyền xem tin tuyển dụng này.");
        }
    }
}
