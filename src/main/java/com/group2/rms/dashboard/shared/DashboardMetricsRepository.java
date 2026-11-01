package com.group2.rms.dashboard.shared;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import com.group2.rms.core.security.RoleAuthorities;
import static com.group2.rms.candidate.domain.repository.ApplicationRepository.FORMAL_FILTER;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/*
 * REPOSITORY: số đếm cho dashboard System Admin (tài khoản, ứng viên, phòng ban) và tổng số hồ sơ chính thức.
 * Số đếm của dashboard nội bộ nằm ở internal/repository/InternalMetricsRepository.
 */
@Repository
public class DashboardMetricsRepository {

    private final EntityManager entityManager;

    public DashboardMetricsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countAccounts() {
        return count("select count(u) from User u", Map.of());
    }

    /** Candidate profiles, independent of account status and number of applications. */
    public long countCandidates() {
        return count("select count(c) from Candidate c", Map.of());
    }

    /** Inclusive rolling window; the upper bound excludes future-dated imported profiles. */
    public long countCandidatesCreatedBetween(LocalDateTime from, LocalDateTime to) {
        return count("select count(c) from Candidate c where c.createdAt >= :from and c.createdAt <= :to",
                Map.of("from", from, "to", to));
    }

    public List<StatusCount> departmentStatuses() {
        return statuses("select d.departmentStatus, count(d) from Department d group by d.departmentStatus", Map.of());
    }

    public List<StatusCount> internalAccountStatuses() {
        return statuses("select u.accountStatus, count(u) from User u where u.role.roleName in :roles "
                + "group by u.accountStatus", Map.of("roles", RoleAuthorities.INTERNAL_ROLE_NAMES));
    }

    public List<StatusCount> candidateAccountStatuses() {
        return statuses("select u.accountStatus, count(u) from User u where u.role.roleName = 'Candidate' "
                + "group by u.accountStatus", Map.of());
    }

    /** All stored internal roles, including roles with no accounts. IDs come from Role. */
    public List<RoleCount> internalAccountsByRole() {
        return entityManager.createQuery("select r.roleId, r.roleName, count(u.userId) from Role r "
                + "left join User u on u.role = r where r.roleName in :roles "
                + "group by r.roleId, r.roleName order by r.roleName", Object[].class)
                .setParameter("roles", RoleAuthorities.INTERNAL_ROLE_NAMES)
                .getResultList().stream()
                .map(row -> new RoleCount((Integer) row[0], (String) row[1], (Long) row[2]))
                .toList();
    }

    /** Tổng số hồ sơ chính thức (không tính HM_Suggested và hồ sơ Talent Pool đã hủy). */
    public long countApplications() {
        return count("select count(a) from Application a where " + FORMAL_FILTER, Map.of());
    }

    private long count(String jpql, Map<String, ?> parameters) {
        var query = entityManager.createQuery(jpql, Long.class);
        parameters.forEach(query::setParameter);
        return query.getSingleResult();
    }

    private List<StatusCount> statuses(String jpql, Map<String, ?> parameters) {
        var query = entityManager.createQuery(jpql, Object[].class);
        parameters.forEach(query::setParameter);
        return query.getResultList().stream()
                .map(row -> new StatusCount((String) row[0], (Long) row[1]))
                .toList();
    }

    public record StatusCount(String status, long count) {
    }

    public record RoleCount(Integer roleId, String roleName, long count) {
    }
}
