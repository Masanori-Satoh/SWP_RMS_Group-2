package com.group2.rms.user.service;

import com.group2.rms.core.util.FieldChanges;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Writes inside the caller's transaction. Credentials and OTPs are never included. */
@Component
@RequiredArgsConstructor
public class UserAuditLogger {
    private final JdbcTemplate jdbc;
    private final UserRepository users;

    private User actor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) throw new AccessDeniedException("Please log in.");
        // The secured caller already checked access. Do not recheck the status after a self-deactivation.
        return users.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Account not found."));
    }

    public void record(User actor, String action, String entity, int id, String before, String after) {
        jdbc.update("INSERT INTO AuditLog (UserId,[Action],EntityName,EntityId,OldValue,NewValue) VALUES (?,?,?,?,?,?)",
                actor.getUserId(), action, entity, Integer.toString(id), before, after);
    }

    public void createdUser(User user, boolean registration) {
        var snapshot = userSnapshot(user);
        record(registration ? user : actor(), "CREATE", "User", user.getUserId(), null,
                (registration ? "Đăng ký tài khoản\n" : "Tạo tài khoản\n")
                        + FieldChanges.describeBefore(snapshot.keySet().stream().toList(), snapshot));
    }

    public void changes(String entity, int id, Map<String, String> before, Map<String, String> after) {
        var changed = FieldChanges.changedKeys(before, after);
        if (!changed.isEmpty()) {
            record(actor(), "UPDATE", entity, id,
                    FieldChanges.describeBefore(changed, before), FieldChanges.describeBefore(changed, after));
        }
    }

    public void createdDepartment(Department department) {
        var snapshot = departmentSnapshot(department);
        record(actor(), "CREATE", "Department", department.getDepartmentId(), null,
                FieldChanges.describeBefore(snapshot.keySet().stream().toList(), snapshot));
    }

    public static Map<String, String> userSnapshot(User user) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("Tên", user.getFullName());
        values.put("Tên đăng nhập", user.getUsername());
        values.put("Email", user.getEmail());
        values.put("Điện thoại", user.getPhoneNumber());
        values.put("Vai trò", user.getRole() == null ? null : user.getRole().getRoleName());
        values.put("Phòng ban", user.getDepartment() == null ? null : user.getDepartment().getDepartmentName());
        values.put("Trạng thái", user.getAccountStatus());
        return values;
    }

    public static Map<String, String> departmentSnapshot(Department department) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("Tên phòng ban", department.getDepartmentName());
        values.put("Trưởng phòng", department.getManager() == null ? null
                : department.getManager().getFullName() + " (#" + department.getManager().getUserId() + ")");
        values.put("Trạng thái", department.getDepartmentStatus());
        return values;
    }
}
