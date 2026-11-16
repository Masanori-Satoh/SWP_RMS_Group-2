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

    // Hằng trạng thái gốc nằm ở entity JobPosting; giữ alias để code cũ không đổi
    public static final String STATUS_DRAFT = JobPosting.STATUS_DRAFT;
    public static final String STATUS_PUBLISHED = JobPosting.STATUS_PUBLISHED;
    public static final String STATUS_PAUSED = JobPosting.STATUS_PAUSED;
    public static final String STATUS_CLOSED = JobPosting.STATUS_CLOSED;

    public static final List<String> OPEN_STATUSES = JobPosting.OPEN_STATUSES;
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

    // Kiểm tra trạng thái cho phép sửa tin.
    public static boolean isEditableStatus(String postingStatus) {
        return EDITABLE_STATUSES.contains(postingStatus);
    }

    // Kiểm tra tin đang đăng hoặc tạm dừng.
    public static boolean isLive(String viewStatus) {
        return STATUS_PUBLISHED.equals(viewStatus) || STATUS_PAUSED.equals(viewStatus);
    }

    // Kiểm tra tin từng hiển thị cho ứng viên.
    public static boolean hasBeenPublished(String viewStatus) {
        return isLive(viewStatus) || STATUS_CLOSED.equals(viewStatus);
    }

    // ----- Người dùng -----

    // Lấy người dùng đã đăng nhập.
    public User actor() {
        return requisitionAccess.actor();
    }

    // Lấy vai trò người dùng.
    public String role(User user) {
        return requisitionAccess.role(user);
    }

    // Kiểm tra quyền quản lý tin.
    public boolean canManage(User user) {
        return MANAGER_ROLES.contains(role(user));
    }

    // Kiểm tra quyền đóng tin.
    public boolean canCloseAny(User user) {
        return RequisitionAccess.ROLE_HR.equals(role(user));
    }

    // Kiểm tra quyền vào trang tin nội bộ.
    public boolean canAccessPostings(User user) {
        return READER_ROLES.contains(role(user));
    }

    // Kiểm tra vai trò quản lý tuyển dụng.
    public boolean isHiringManager(User user) {
        return RequisitionAccess.ROLE_HIRING_MANAGER.equals(role(user));
    }

    // Kiểm tra quyền xem ứng viên tiềm năng.
    public boolean canSeeTalentPool(User user) {
        return TALENT_POOL_ROLES.contains(role(user));
    }

    // Kiểm tra quyền xem tin.
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

    // Kiểm tra quyền đăng tin.
    public boolean canPublish(User user, String viewStatus) {
        return canManage(user) && STATUS_DRAFT.equals(viewStatus);
    }

    // Kiểm tra quyền mở lại tin.
    public boolean canReopen(User user, String viewStatus) {
        return canManage(user) && STATUS_PAUSED.equals(viewStatus);
    }

    // Kiểm tra quyền sửa tin.
    public boolean canEdit(User user, String viewStatus) {
        return canManage(user) && viewStatus != null && !STATUS_CLOSED.equals(viewStatus);
    }

    // Kiểm tra quyền tạm dừng tin.
    public boolean canPause(User user, String viewStatus) {
        return canManage(user) && STATUS_PUBLISHED.equals(viewStatus);
    }

    // Kiểm tra quyền đóng tin cụ thể.
    public boolean canClose(User user, String viewStatus) {
        return canCloseAny(user) && isLive(viewStatus);
    }

    // Kiểm tra quyền xóa tin.
    public boolean canDelete(User user, String viewStatus, boolean hasApplications) {
        return canManage(user) && STATUS_DRAFT.equals(viewStatus) && !hasApplications;
    }

    // Kiểm tra quyền đăng lại tin.
    public boolean canRepost(User user, String viewStatus, JobRequisition requisition, boolean hasOpenPosting) {
        return canManage(user) && STATUS_CLOSED.equals(viewStatus) && requisition != null
                && RequisitionAccess.STATUS_APPROVED.equals(requisition.getApprovalStatus()) && !hasOpenPosting;
    }

    // ----- requireX: ném AccessDeniedException (trang 403) -----

    // Yêu cầu quyền quản lý tin.
    public void requireManage(User user) {
        if (!canManage(user)) {
            throw new AccessDeniedException("Chỉ HR mới có quyền quản lý tin tuyển dụng.");
        }
    }

    // Yêu cầu quyền vào trang tin nội bộ.
    public void requireAccessPostings(User user) {
        if (!canAccessPostings(user)) {
            throw new AccessDeniedException("Bạn không có quyền xem tin tuyển dụng.");
        }
    }

    // Yêu cầu quyền đóng tin.
    public void requireCloseAny(User user) {
        if (!canCloseAny(user)) {
            throw new AccessDeniedException("Chỉ HR được đóng đợt tuyển dụng.");
        }
    }

    // Yêu cầu quyền xem tin.
    public void requireView(User user, JobPosting posting) {
        requireAccessPostings(user);
        if (!canView(user, posting)) {
            throw new AccessDeniedException("Bạn không có quyền xem tin tuyển dụng này.");
        }
    }
}
