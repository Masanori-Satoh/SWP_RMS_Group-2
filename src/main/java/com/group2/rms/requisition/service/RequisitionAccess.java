package com.group2.rms.requisition.service;

import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.exception.RequisitionStateException;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RequisitionAccess {

    public static final String ROLE_HIRING_MANAGER = "Hiring Manager";
    public static final String ROLE_DIRECTOR = "Director";
    public static final String ROLE_HR = "HR";
    public static final String ROLE_SYSTEM_ADMIN = "System Admin";

    public static final String STATUS_DRAFT = JobRequisition.STATUS_DRAFT;
    public static final String STATUS_PENDING_DIRECTOR = JobRequisition.STATUS_PENDING_DIRECTOR;
    public static final String STATUS_APPROVED = JobRequisition.STATUS_APPROVED;
    public static final String STATUS_REJECTED = JobRequisition.STATUS_REJECTED;
    public static final String STATUS_CLOSED = JobRequisition.STATUS_CLOSED;

    private static final Set<String> ALLOWED_ROLES = Set.of(
            ROLE_HIRING_MANAGER,
            ROLE_DIRECTOR,
            ROLE_HR);

    private final UserRepository userRepository;


    // Lấy người dùng đã xác thực.
    public User actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Vui lòng đăng nhập để tiếp tục.");
        }

        User currentUser = userRepository.findByUsernameIgnoreCase(authentication.getName())
                .filter(user -> "Active".equals(user.getAccountStatus()))
                .orElseThrow(() -> new AccessDeniedException("Tài khoản không hoạt động hoặc không tồn tại."));

        String userRole = role(currentUser);
        if (!ALLOWED_ROLES.contains(userRole)) {
            throw new AccessDeniedException("Vai trò hiện tại không có quyền truy cập Requisition.");
        }

        return currentUser;
    }

    // Lấy vai trò của người dùng.
    public String role(User user) {
        if (user == null || user.getRole() == null) {
            return "";
        }
        return user.getRole().getRoleName();
    }

    // Kiểm tra người tạo yêu cầu.
    public boolean owns(User user, JobRequisition requisition) {
        if (user == null || requisition == null || requisition.getHiringManager() == null) {
            return false;
        }
        return Objects.equals(user.getUserId(), requisition.getHiringManager().getUserId());
    }
    // Kiểm tra vai trò Giám đốc.
    public boolean isDirector(User user) {
        return ROLE_DIRECTOR.equals(role(user));
    }
    // Kiểm tra quyền tạo yêu cầu.
    public boolean canCreate(User user) {
        String userRole = role(user);
        return Set.of(ROLE_HIRING_MANAGER, ROLE_DIRECTOR).contains(userRole);
    }
    // Kiểm tra quyền tạo tin tuyển dụng.
    public boolean canCreatePosting(User user) {
        String userRole = role(user);
        return ROLE_HR.equals(userRole);
    }
    // Kiểm tra quyền xem tin tuyển dụng.
    public boolean canViewPosting(User user) {
        String userRole = role(user);
        return ROLE_HR.equals(userRole) || ROLE_HIRING_MANAGER.equals(userRole) || ROLE_DIRECTOR.equals(userRole);
    }
    // Kiểm tra quyền sửa yêu cầu.
    public boolean canEdit(User user, JobRequisition requisition) {
        if (requisition == null) {
            return false;
        }
        boolean isOwner = owns(user, requisition) && canCreate(user);
        boolean isEditableStatus = Set.of(STATUS_DRAFT, STATUS_REJECTED).contains(requisition.getApprovalStatus());
        return isOwner && isEditableStatus;
    }


    // Kiểm tra quyền xóa yêu cầu.
    public boolean canDelete(User user, JobRequisition requisition, boolean hasPosting, boolean hasLaterRound) {
        return canEdit(user, requisition) && STATUS_DRAFT.equals(requisition.getApprovalStatus())
                && !hasPosting && !hasLaterRound;
    }
    // Kiểm tra quyền sao chép yêu cầu.
    public boolean canCopy(User user, JobRequisition requisition) {
        if (requisition == null) {
            return false;
        }
        return canCreate(user) && !STATUS_DRAFT.equals(requisition.getApprovalStatus());
    }
    // Kiểm tra quyền tạo tin từ yêu cầu.
    public boolean canCreatePostingFrom(User user, JobRequisition requisition, boolean hasPosting) {
        if (requisition == null || hasPosting) {
            return false;
        }
        return STATUS_APPROVED.equals(requisition.getApprovalStatus()) && canCreatePosting(user);
    }


    // Kiểm tra quyền mở tin tuyển dụng.
    public boolean canOpenPosting(User user, String latestPostingStatus) {
        if (latestPostingStatus == null || !canViewPosting(user)) {
            return false;
        }
        if (RequisitionLabels.POSTING_DRAFT.equals(latestPostingStatus)) {
            return canCreatePosting(user);
        }
        return true;
    }
    // Kiểm tra quyền phê duyệt yêu cầu.
    public boolean canDecide(User user, JobRequisition requisition) {
        if (requisition == null) {
            return false;
        }
        boolean isDirector = ROLE_DIRECTOR.equals(role(user));
        boolean isNotOwner = !owns(user, requisition);
        boolean isPending = STATUS_PENDING_DIRECTOR.equals(requisition.getApprovalStatus());
        return isDirector && isNotOwner && isPending;
    }

    // Kiểm tra quyền đóng yêu cầu.
    public boolean canClose(User user, JobRequisition requisition) {
        return requisition != null && STATUS_APPROVED.equals(requisition.getApprovalStatus())
                && (isDirector(user) || owns(user, requisition) && ROLE_HIRING_MANAGER.equals(role(user)));
    }


    // Yêu cầu quyền tạo yêu cầu.
    public void requireCreate(User user) {
        if (!canCreate(user)) {
            throw new AccessDeniedException("Chỉ Hiring Manager và Director mới có quyền tạo yêu cầu tuyển dụng.");
        }
    }
    // Kiểm tra yêu cầu thuộc phòng ban được quản lý.
    public boolean inManagedDepartment(User user, JobRequisition requisition) {
        if (requisition == null || requisition.getDepartment() == null) {
            return false;
        }
        return canManageDepartment(user, requisition.getDepartment().getDepartmentId(),
                requisition.getDepartment().getManager());
    }
    // Kiểm tra quyền quản lý phòng ban.
    public boolean canManageDepartment(User user, Integer departmentId, User departmentManager) {
        if (departmentId == null || user == null) {
            return false;
        }
        String userRole = role(user);
        if (ROLE_DIRECTOR.equals(userRole)) {
            return true;
        }
        if (ROLE_HIRING_MANAGER.equals(userRole)) {
            if (departmentManager != null && Objects.equals(user.getUserId(), departmentManager.getUserId())) {
                return true;
            }
            if (user.getDepartment() != null && Objects.equals(user.getDepartment().getDepartmentId(), departmentId)) {
                return true;
            }
        }
        return false;
    }
    // Yêu cầu quyền xem yêu cầu.
    public void requireView(User user, JobRequisition requisition) {
        if (requisition == null) {
            throw new AccessDeniedException("Yêu cầu tuyển dụng không tồn tại.");
        }
        String userRole = role(user);
        if (ROLE_HR.equals(userRole)) {
            if (STATUS_APPROVED.equals(requisition.getApprovalStatus())) {
                return;
            }
            throw new AccessDeniedException("HR chỉ có quyền xem các yêu cầu tuyển dụng đã được phê duyệt.");
        }
        if (canCreate(user) && owns(user, requisition)) {
            return;
        }
        if (ROLE_DIRECTOR.equals(userRole) && !STATUS_DRAFT.equals(requisition.getApprovalStatus())) {
            return;
        }
        if (ROLE_HIRING_MANAGER.equals(userRole) && inManagedDepartment(user, requisition)) {
            return;
        }
        throw new AccessDeniedException("Bạn không có quyền truy cập yêu cầu tuyển dụng này.");
    }
    // Yêu cầu quyền sửa yêu cầu.
    public void requireEdit(User user, JobRequisition requisition) {
        requireView(user, requisition);
        boolean isOwner = owns(user, requisition) && canCreate(user);
        if (!isOwner) {
            throw new AccessDeniedException("Chỉ người tạo mới có quyền chỉnh sửa hoặc xóa yêu cầu này.");
        }
        if (!canEdit(user, requisition)) {
            throw new RequisitionStateException();
        }
    }
    // Yêu cầu quyền phê duyệt yêu cầu.
    public void requireDecide(User user, JobRequisition requisition) {
        requireView(user, requisition);
        if (!isDirector(user) || owns(user, requisition)) {
            throw new AccessDeniedException("Chỉ Giám đốc mới có quyền quyết định phê duyệt yêu cầu.");
        }
        if (!canDecide(user, requisition)) {
            throw new RequisitionStateException();
        }
    }
    // Yêu cầu quyền sao chép yêu cầu.
    public void requireCopy(User user, JobRequisition requisition) {
        requireCreate(user);
        requireView(user, requisition);
        if (!canCopy(user, requisition)) {
            throw new RequisitionStateException();
        }
    }
}
