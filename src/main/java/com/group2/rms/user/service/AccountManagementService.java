package com.group2.rms.user.service;

import com.group2.rms.candidate.Candidate;
import com.group2.rms.candidate.CandidateRepository;
import com.group2.rms.core.security.RoleAuthorities;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.exception.AccountFieldException;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

@Service
public class AccountManagementService {

    private static final Set<String> STATUSES = Set.of("Active", "Inactive", "Blocked");

    private final UserRepository users;
    private final RoleRepository roles;
    private final DepartmentRepository departments;
    private final CandidateRepository candidates;
    private final PasswordEncoder passwordEncoder;

    public AccountManagementService(UserRepository users, RoleRepository roles,
                                    DepartmentRepository departments, CandidateRepository candidates,
                                    PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.departments = departments;
        this.candidates = candidates;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AccountForEdit findForEdit(int userId) {
        User user = findUser(userId);
        return new AccountForEdit(user.getUsername(), user.getFullName(), user.getEmail(),
                user.getPhoneNumber(), user.getRole().getRoleId(),
                user.getDepartment() == null ? null : user.getDepartment().getDepartmentId(),
                user.getAccountStatus());
    }

    @Transactional
    public int create(CreateCommand command) {
        String fullName = required(command.fullName(), "fullName", "Vui lòng nhập họ và tên.");
        String username = required(command.username(), "username", "Vui lòng nhập username.");
        String email = required(command.email(), "email", "Vui lòng nhập email.");
        String phone = optional(command.phoneNumber());
        validatePassword(command.password());
        Role role = findRole(command.roleId());
        Department department = findDepartment(command.departmentId(), role);
        checkUsernameAvailable(username, null);
        checkEmailAvailable(email, null);
        User user = users.saveAndFlush(User.builder()
                .fullName(fullName)
                .username(username)
                .email(email)
                .phoneNumber(phone)
                .role(role)
                .department(department)
                .passwordHash(passwordEncoder.encode(command.password()))
                .accountStatus("Active")
                .build());

        if (isCandidate(role)) {
            candidates.save(newCandidateProfile(user));
        }
        return user.getUserId();
    }

    @Transactional
    public void update(int userId, UpdateCommand command) {
        User user = findUser(userId);
        String fullName = required(command.fullName(), "fullName", "Vui lòng nhập họ và tên.");
        String email = required(command.email(), "email", "Vui lòng nhập email.");
        String phone = optional(command.phoneNumber());
        Role role = findRole(command.roleId());
        Department department = findDepartment(command.departmentId(), role);
        if (command.accountStatus() == null || !STATUSES.contains(command.accountStatus())) {
            throw new AccountFieldException("accountStatus", "Trạng thái tài khoản không hợp lệ.");
        }
        checkEmailAvailable(email, userId);

        boolean hasCandidateProfile = candidates.findByAccountUserId(userId).isPresent();

        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setRole(role);
        user.setDepartment(department);
        user.setAccountStatus(command.accountStatus());
        users.saveAndFlush(user);

        if (!hasCandidateProfile && isCandidate(role)) {
            candidates.save(newCandidateProfile(user));
        }
        // Username and passwordHash are intentionally never assigned here.
    }

    private User findUser(int userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private Role findRole(Integer roleId) {
        Role role = roleId == null ? null : roles.findById(roleId).orElse(null);
        if (role == null) {
            throw new AccountFieldException("roleId", "Vai trò không hợp lệ.");
        }
        try {
            RoleAuthorities.fromRoleName(role.getRoleName());
        } catch (IllegalArgumentException ex) {
            throw new AccountFieldException("roleId", "Vai trò không được hỗ trợ.");
        }
        return role;
    }

    private Department findDepartment(Integer departmentId, Role role) {
        if (departmentId == null) {
            if (!isCandidate(role)) {
                throw new AccountFieldException("departmentId", "Vai trò nội bộ cần phòng ban.");
            }
            return null;
        }
        return departments.findById(departmentId)
                .orElseThrow(() -> new AccountFieldException("departmentId", "Phòng ban không hợp lệ."));
    }

    private void checkUsernameAvailable(String username, Integer currentId) {
        if (belongsToAnother(users.findByUsernameIgnoreCase(username), currentId)
                || belongsToAnother(users.findByEmailIgnoreCase(username), currentId)) {
            throw new AccountFieldException("username", "Username đã được sử dụng làm tên đăng nhập hoặc email.");
        }
    }

    private void checkEmailAvailable(String email, Integer currentId) {
        if (belongsToAnother(users.findByEmailIgnoreCase(email), currentId)
                || belongsToAnother(users.findByUsernameIgnoreCase(email), currentId)) {
            throw new AccountFieldException("email", "Email đã được sử dụng làm email hoặc username.");
        }
    }

    private static boolean belongsToAnother(Optional<User> found, Integer currentId) {
        return found.isPresent() && !found.get().getUserId().equals(currentId);
    }

    private static Candidate newCandidateProfile(User user) {
        return Candidate.builder().account(user).build();
    }

    private static boolean isCandidate(Role role) {
        return "Candidate".equals(role.getRoleName());
    }

    private static String required(String value, String field, String message) {
        String normalized = optional(value);
        if (normalized == null) {
            throw new AccountFieldException(field, message);
        }
        return normalized;
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank() || password.length() < 8 || password.length() > 32) {
            throw new AccountFieldException("password", "Mật khẩu phải từ 8 đến 32 ký tự.");
        }
    }

    public record CreateCommand(String fullName, String username, String email, String phoneNumber,
                                Integer roleId, Integer departmentId, String password) {
    }

    public record UpdateCommand(String fullName, String email, String phoneNumber,
                                Integer roleId, Integer departmentId, String accountStatus) {
    }

    public record AccountForEdit(String username, String fullName, String email, String phoneNumber,
                                 Integer roleId, Integer departmentId, String accountStatus) {
    }
}
