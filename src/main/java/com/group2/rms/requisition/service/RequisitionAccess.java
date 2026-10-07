package com.group2.rms.requisition.service;

import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

/**
 * Phân quyền và kiểm tra phạm vi truy cập cho Job Requisition.
 * Tuân thủ quy định tại docs/members/hoangnh/rule_code_nhh.md
 */
@Component
@RequiredArgsConstructor
public class RequisitionAccess {

    public static final String ROLE_HIRING_MANAGER = "Hiring Manager";
    public static final String ROLE_DIRECTOR = "Director";
    public static final String ROLE_HR = "HR";
    public static final String ROLE_SYSTEM_ADMIN = "System Admin";

    public static final String STATUS_DRAFT = "Draft";
    public static final String STATUS_PENDING_DIRECTOR = "Pending_Director";
    public static final String STATUS_APPROVED = "Approved";
    public static final String STATUS_REJECTED = "Rejected";

    private static final Set<String> ALLOWED_ROLES = Set.of(
            ROLE_HIRING_MANAGER,
            ROLE_DIRECTOR,
            ROLE_HR,
            ROLE_SYSTEM_ADMIN
    );

    private final UserRepository userRepository;

    /**
     * Lấy thông tin người dùng đang đăng nhập từ SecurityContext.
     */
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

    public String role(User user) {
        if (user == null || user.getRole() == null) {
            return "";
        }
        return user.getRole().getRoleName();
    }

    public boolean owns(User user, JobRequisition requisition) {
        if (user == null || requisition == null || requisition.getHiringManager() == null) {
            return false;
        }
        return Objects.equals(user.getUserId(), requisition.getHiringManager().getUserId());
    }

    // Security: Hiring Manager, Director và System Admin có quyền tạo Requisition
    public boolean canCreate(User user) {
        String userRole = role(user);
        return Set.of(ROLE_HIRING_MANAGER, ROLE_DIRECTOR, ROLE_SYSTEM_ADMIN).contains(userRole);
    }

    // Security: Chỉ người tạo hoặc Admin mới được sửa Requisition khi ở trạng thái Draft hoặc Rejected
    public boolean canEdit(User user, JobRequisition requisition) {
        if (requisition == null) {
            return false;
        }
        boolean isOwnerOrAdmin = owns(user, requisition) || ROLE_SYSTEM_ADMIN.equals(role(user));
        boolean isEditableStatus = Set.of(STATUS_DRAFT, STATUS_REJECTED).contains(requisition.getApprovalStatus());
        return isOwnerOrAdmin && isEditableStatus;
    }

    // Security: Chỉ Director (không phải người tạo) mới được phê duyệt/từ chối khi trạng thái Pending_Director
    public boolean canDecide(User user, JobRequisition requisition) {
        if (requisition == null) {
            return false;
        }
        boolean isDirector = ROLE_DIRECTOR.equals(role(user));
        boolean isNotOwner = !owns(user, requisition);
        boolean isPending = STATUS_PENDING_DIRECTOR.equals(requisition.getApprovalStatus());
        return isDirector && isNotOwner && isPending;
    }

    // Security: Yêu cầu quyền tạo Requisition, ném ngoại lệ nếu không đủ thẩm quyền
    public void requireCreate(User user) {
        if (!canCreate(user)) {
            throw new AccessDeniedException("Chỉ Hiring Manager, Director và Quản trị viên mới có quyền tạo yêu cầu tuyển dụng.");
        }
    }

    // Security: Kiểm tra requisition có thuộc phòng ban mà HM phụ trách hay không
    public boolean inManagedDepartment(User user, JobRequisition requisition) {
        if (requisition == null || requisition.getDepartment() == null) {
            return false;
        }
        return canManageDepartment(user, requisition.getDepartment().getDepartmentId(), requisition.getDepartment().getManager());
    }

    // Security: HM chỉ quản lý phòng ban mình làm trưởng phòng (managerId) hoặc trực thuộc
    public boolean canManageDepartment(User user, Integer departmentId, User departmentManager) {
        if (departmentId == null || user == null) {
            return false;
        }
        String userRole = role(user);
        if (Set.of(ROLE_DIRECTOR, ROLE_SYSTEM_ADMIN).contains(userRole)) {
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

    // Security: Phân quyền xem chi tiết requisition theo phạm vi vai trò
    public void requireView(User user, JobRequisition requisition) {
        if (requisition == null) {
            throw new AccessDeniedException("Yêu cầu tuyển dụng không tồn tại.");
        }
        String userRole = role(user);
        if (ROLE_SYSTEM_ADMIN.equals(userRole)) {
            return;
        }
        // Security: HR chỉ có quyền xem các yêu cầu tuyển dụng đã được phê duyệt (Approved)
        if (ROLE_HR.equals(userRole)) {
            if (STATUS_APPROVED.equals(requisition.getApprovalStatus())) {
                return;
            }
            throw new AccessDeniedException("HR chỉ có quyền xem các yêu cầu tuyển dụng đã được phê duyệt.");
        }
        if (owns(user, requisition)) {
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

    // Security: Kiểm tra quyền chỉnh sửa Requisition, ném ngoại lệ nếu không đủ thẩm quyền
    public void requireEdit(User user, JobRequisition requisition) {
        requireView(user, requisition);
        if (!canEdit(user, requisition)) {
            throw new AccessDeniedException("Chỉ có thể chỉnh sửa hoặc xóa yêu cầu ở trạng thái Bản nháp hoặc Từ chối.");
        }
    }
}
