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

    private static final Set<String> ALLOWED_ROLES = Set.of(
            ROLE_HIRING_MANAGER,
            ROLE_DIRECTOR,
            ROLE_HR);

    private final UserRepository userRepository;


    // Returns the authenticated user.
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

    // Returns the user's role.
    public String role(User user) {
        if (user == null || user.getRole() == null) {
            return "";
        }
        return user.getRole().getRoleName();
    }

    // Checks whether the user owns the requisition.
    public boolean owns(User user, JobRequisition requisition) {
        if (user == null || requisition == null || requisition.getHiringManager() == null) {
            return false;
        }
        return Objects.equals(user.getUserId(), requisition.getHiringManager().getUserId());
    }
    // Checks whether the user is a Director.
    public boolean isDirector(User user) {
        return ROLE_DIRECTOR.equals(role(user));
    }
    // Checks whether the user can create a requisition.
    public boolean canCreate(User user) {
        String userRole = role(user);
        return Set.of(ROLE_HIRING_MANAGER, ROLE_DIRECTOR).contains(userRole);
    }
    // Checks whether the user can create a job posting.
    public boolean canCreatePosting(User user) {
        String userRole = role(user);
        return ROLE_HR.equals(userRole);
    }
    // Checks whether the user can view job postings.
    public boolean canViewPosting(User user) {
        String userRole = role(user);
        return ROLE_HR.equals(userRole) || ROLE_HIRING_MANAGER.equals(userRole) || ROLE_DIRECTOR.equals(userRole);
    }
    // Checks whether the user can edit the requisition.
    public boolean canEdit(User user, JobRequisition requisition) {
        if (requisition == null) {
            return false;
        }
        boolean isOwner = owns(user, requisition) && canCreate(user);
        boolean isEditableStatus = Set.of(STATUS_DRAFT, STATUS_REJECTED).contains(requisition.getApprovalStatus());
        return isOwner && isEditableStatus;
    }


    // Checks whether the requisition can be deleted.
    public boolean canDelete(User user, JobRequisition requisition, boolean hasPosting, boolean hasLaterRound) {
        return canEdit(user, requisition) && STATUS_DRAFT.equals(requisition.getApprovalStatus())
                && !hasPosting && !hasLaterRound;
    }
    // Checks whether the requisition can be copied.
    public boolean canCopy(User user, JobRequisition requisition) {
        if (requisition == null) {
            return false;
        }
        return canCreate(user) && STATUS_APPROVED.equals(requisition.getApprovalStatus());
    }
    // Checks whether a posting can be created from the requisition.
    public boolean canCreatePostingFrom(User user, JobRequisition requisition, boolean hasPosting) {
        if (requisition == null || hasPosting) {
            return false;
        }
        return STATUS_APPROVED.equals(requisition.getApprovalStatus()) && canCreatePosting(user);
    }


    // Checks whether the user can open the posting.
    public boolean canOpenPosting(User user, String latestPostingStatus) {
        if (latestPostingStatus == null || !canViewPosting(user)) {
            return false;
        }
        if (RequisitionLabels.POSTING_DRAFT.equals(latestPostingStatus)) {
            return canCreatePosting(user);
        }
        return true;
    }
    // Checks whether the user can decide on the requisition.
    public boolean canDecide(User user, JobRequisition requisition) {
        if (requisition == null) {
            return false;
        }
        boolean isDirector = ROLE_DIRECTOR.equals(role(user));
        boolean isNotOwner = !owns(user, requisition);
        boolean isPending = STATUS_PENDING_DIRECTOR.equals(requisition.getApprovalStatus());
        return isDirector && isNotOwner && isPending;
    }


    // Requires permission to create a requisition.
    public void requireCreate(User user) {
        if (!canCreate(user)) {
            throw new AccessDeniedException("Chỉ Hiring Manager và Director mới có quyền tạo yêu cầu tuyển dụng.");
        }
    }
    // Checks whether the requisition belongs to a managed department.
    public boolean inManagedDepartment(User user, JobRequisition requisition) {
        if (requisition == null || requisition.getDepartment() == null) {
            return false;
        }
        return canManageDepartment(user, requisition.getDepartment().getDepartmentId(),
                requisition.getDepartment().getManager());
    }
    // Checks whether the user manages the department.
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
    // Requires permission to view the requisition.
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
    // Requires permission to edit the requisition.
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
    // Requires permission to decide on the requisition.
    public void requireDecide(User user, JobRequisition requisition) {
        requireView(user, requisition);
        if (!isDirector(user) || owns(user, requisition)) {
            throw new AccessDeniedException("Chỉ Giám đốc mới có quyền quyết định phê duyệt yêu cầu.");
        }
        if (!canDecide(user, requisition)) {
            throw new RequisitionStateException();
        }
    }
    // Requires permission to copy the requisition.
    public void requireCopy(User user, JobRequisition requisition) {
        requireCreate(user);
        requireView(user, requisition);
        if (!canCopy(user, requisition)) {
            throw new RequisitionStateException();
        }
    }
}
