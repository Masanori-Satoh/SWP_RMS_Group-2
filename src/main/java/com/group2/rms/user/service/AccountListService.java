package com.group2.rms.user.service;

import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.candidate.domain.repository.CandidateRepository;
import com.group2.rms.core.security.RoleAuthorities;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AccountListService {

    private static final int PAGE_SIZE = 10;
    private static final Set<String> STATUSES = Set.of("Active", "Inactive");

    private final UserRepository users;
    private final RoleRepository roles;
    private final DepartmentRepository departments;
    private final CandidateRepository candidates;
    private final UserAuditLogger audit;

    public AccountListService(UserRepository users, RoleRepository roles, DepartmentRepository departments,
            CandidateRepository candidates, UserAuditLogger audit) {
        this.users = users;
        this.roles = roles;
        this.departments = departments;
        this.candidates = candidates;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<AccountRow> findAccounts(String search, Integer roleId, Integer departmentId,
            String status, String sort, int page) {
        return findUsers(search, roleId, departmentId, status, sort, page, false)
                .map(user -> new AccountRow(user.getUserId(), user.getFullName(), user.getEmail(),
                        user.getUsername(), user.getRole().getRoleName(),
                        user.getDepartment() == null ? null : user.getDepartment().getDepartmentName(),
                        user.getAccountStatus(), details(user, null)));
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<CandidateRow> findCandidateAccounts(String search, String status, String sort, int page) {
        Page<User> result = findUsers(search, null, null, status, sort, page, true);
        java.util.Map<Integer, Candidate> profiles = new java.util.HashMap<>();
        if (!result.isEmpty()) {
            candidates.findAllByAccountUserIdIn(result.stream().map(User::getUserId).toList())
                    .forEach(profile -> profiles.put(profile.getAccount().getUserId(), profile));
        }
        return result.map(user -> new CandidateRow(user.getUserId(), user.getFullName(), user.getEmail(),
                user.getUsername(), user.getAccountStatus(), user.getCreatedAt(),
                profiles.containsKey(user.getUserId()) ? profiles.get(user.getUserId()).getCandidateId() : null,
                details(user, profiles.get(user.getUserId()))));
    }

    // Read-only presentation data from the same scoped page and existing batched
    // profile query.
    // Never expose entities, credentials, or authentication metadata to the
    // inspector.
    private static InspectorDetails details(User user, Candidate profile) {
        ProfileDetails linked = profile == null ? null
                : new ProfileDetails(profile.getCandidateId(),
                        profile.getDateOfBirth(), profile.getGender(), profile.getAddress(),
                        profile.getCandidateSource(), profile.getCreatedAt());
        return new InspectorDetails(user.getPhoneNumber(), user.getCreatedAt(), user.getUpdatedAt(), linked);
    }

    private Page<User> findUsers(String search, Integer roleId, Integer departmentId,
            String status, String sort, int page, boolean candidate) {
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        Specification<User> specification = (root, query, builder) -> {
            List<Predicate> filters = new ArrayList<>();
            filters.add(candidate ? builder.equal(root.get("role").get("roleName"), "Candidate")
                    : root.get("role").get("roleName").in(RoleAuthorities.INTERNAL_ROLE_NAMES));
            if (!term.isEmpty()) {
                String pattern = "%" + escapeLike(term) + "%";
                filters.add(builder.or(
                        builder.like(builder.lower(root.get("fullName")), pattern, '\\'),
                        builder.like(builder.lower(root.get("email")), pattern, '\\'),
                        builder.like(builder.lower(root.get("username")), pattern, '\\')));
            }
            if (roleId != null) {
                filters.add(builder.equal(root.get("role").get("roleId"), roleId));
            }
            if (departmentId != null) {
                filters.add(builder.equal(root.get("department").get("departmentId"), departmentId));
            }
            if (status != null && STATUSES.contains(status)) {
                filters.add(builder.equal(root.get("accountStatus"), status));
            }
            return builder.and(filters.toArray(Predicate[]::new));
        };

        Sort order = switch (sort == null ? "" : sort) {
            case "name_desc" -> Sort.by(Sort.Order.desc("fullName"), Sort.Order.asc("userId"));
            case "username" -> Sort.by(Sort.Order.asc("username"), Sort.Order.asc("userId"));
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("userId"));
            default -> Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("userId"));
        };
        return users.findAll(specification, PageRequest.of(Math.max(page, 0), PAGE_SIZE, order));
    }

    @Transactional(readOnly = true)
    public List<Role> findRoles() {
        return roles.findAll(Sort.by("roleName")).stream()
                .filter(role -> RoleAuthorities.INTERNAL_ROLE_NAMES.contains(role.getRoleName())).toList();
    }

    @Transactional(readOnly = true)
    public List<Department> findDepartments() {
        return departments.findAll(Sort.by("departmentName"));
    }

    @Transactional
    public void deactivate(int userId) {
        deactivateScoped(userId, false);
    }

    @Transactional
    public void deactivateCandidate(int userId) {
        deactivateScoped(userId, true);
    }

    @Transactional
    public void activate(int userId) {
        activateScoped(userId, false);
    }

    @Transactional
    public void activateCandidate(int userId) {
        activateScoped(userId, true);
    }

    private void activateScoped(int userId, boolean candidate) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        if (candidate ? !"Candidate".equals(user.getRole().getRoleName())
                : !RoleAuthorities.INTERNAL_ROLE_NAMES.contains(user.getRole().getRoleName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản không thuộc nhóm này.");
        }
        if (!"Active".equals(user.getAccountStatus()) && !"Inactive".equals(user.getAccountStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trạng thái tài khoản không hợp lệ.");
        }
        if ("Inactive".equals(user.getAccountStatus())) {
            var before = UserAuditLogger.userSnapshot(user);
            user.setAccountStatus("Active");
            users.save(user);
            audit.changes("User", userId, before, UserAuditLogger.userSnapshot(user));
        }
    }

    private void deactivateScoped(int userId, boolean candidate) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        if (candidate ? !"Candidate".equals(user.getRole().getRoleName())
                : !RoleAuthorities.INTERNAL_ROLE_NAMES.contains(user.getRole().getRoleName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found in this account group");
        }
        if (!"Inactive".equals(user.getAccountStatus())) {
            var before = UserAuditLogger.userSnapshot(user);
            user.setAccountStatus("Inactive");
            users.save(user);
            audit.changes("User", userId, before, UserAuditLogger.userSnapshot(user));
        }
    }

    private static String escapeLike(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    //
    public record AccountRow(Integer id, String fullName, String email, String username, String roleName,
            String departmentName, String status, InspectorDetails details) {
        public AccountRow(Integer id, String fullName, String email, String username,
                String roleName, String departmentName, String status) {
            this(id, fullName, email, username, roleName, departmentName, status, null);
        }
    }

    public record CandidateRow(Integer id, String fullName, String email, String username,
            String status, java.time.LocalDateTime createdAt, Integer profileId, InspectorDetails details) {
        public CandidateRow(Integer id, String fullName, String email, String username,
                String status, java.time.LocalDateTime createdAt, Integer profileId) {
            this(id, fullName, email, username, status, createdAt, profileId, null);
        }
    }

    // detail of a account
    public record InspectorDetails(String phoneNumber, LocalDateTime createdAt, LocalDateTime updatedAt,
            ProfileDetails profile) {
    }

    public record ProfileDetails(Integer id, LocalDate dateOfBirth, String gender,
            String address, String source, java.time.LocalDateTime createdAt) {
    }
}
