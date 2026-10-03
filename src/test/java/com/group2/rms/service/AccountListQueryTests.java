package com.group2.rms.service;

import com.group2.rms.candidate.repository.CandidateRepository;
import com.group2.rms.core.security.RoleAuthorities;
import com.group2.rms.dashboard.repository.DashboardMetricsRepository;
import com.group2.rms.dashboard.service.DashboardService;
import com.group2.rms.user.service.AccountListService;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional(readOnly = true)
class AccountListQueryTests {

    @Autowired private AccountListService accounts;
    @Autowired private UserRepository users;
    @Autowired private DashboardMetricsRepository metrics;
    @Autowired private DashboardService dashboard;
    @Autowired private CandidateRepository candidates;

    @Test
    void listUsesUserAccountsAndFiltersStoredFields() {
        Page<AccountListService.AccountRow> all = accounts.findAccounts("", null, null, "", "name", 0);
        var storedUsers = users.findAll();
        long internalCount = storedUsers.stream().filter(user -> RoleAuthorities.INTERNAL_ROLE_NAMES.contains(user.getRole().getRoleName())).count();
        long candidateCount = storedUsers.stream().filter(user -> "Candidate".equals(user.getRole().getRoleName())).count();
        assertEquals(internalCount, all.getTotalElements());
        assertTrue(all.stream().noneMatch(row -> "Candidate".equals(row.roleName())));
        assertTrue(accounts.findRoles().stream().noneMatch(role -> "Candidate".equals(role.getRoleName())));
        var candidatePage = accounts.findCandidateAccounts("", "", "name", 0);
        candidatePage.forEach(row -> {
            var linked = candidates.findByAccountUserId(row.id());
            assertEquals(linked.map(profile -> profile.getCandidateId()).orElse(null), row.profileId());
            assertEquals(users.findById(row.id()).orElseThrow().getCreatedAt(), row.createdAt());
        });
        assertEquals(candidateCount, candidatePage.getTotalElements());
        assertEquals(internalCount, metrics.internalAccountStatuses().stream().mapToLong(row -> row.count()).sum());
        assertEquals(candidateCount, metrics.candidateAccountStatuses().stream().mapToLong(row -> row.count()).sum());
        for (String status : java.util.List.of("Active", "Inactive", "Blocked")) {
            long expectedInternal = storedUsers.stream().filter(user -> RoleAuthorities.INTERNAL_ROLE_NAMES.contains(user.getRole().getRoleName()) && status.equals(user.getAccountStatus())).count();
            long expectedCandidate = storedUsers.stream().filter(user -> "Candidate".equals(user.getRole().getRoleName()) && status.equals(user.getAccountStatus())).count();
            assertEquals(expectedInternal, accounts.findAccounts("", null, null, status, "name", 0).getTotalElements());
            assertEquals(expectedCandidate, accounts.findCandidateAccounts("", status, "name", 0).getTotalElements());
            assertEquals(expectedInternal, metrics.internalAccountStatuses().stream().filter(row -> status.equals(row.status())).mapToLong(row -> row.count()).sum());
            assertEquals(expectedCandidate, metrics.candidateAccountStatuses().stream().filter(row -> status.equals(row.status())).mapToLong(row -> row.count()).sum());
        }
        storedUsers.stream().filter(user -> "System Admin".equals(user.getRole().getRoleName()) && "Active".equals(user.getAccountStatus())).findFirst().ifPresent(admin -> {
            var groups = dashboard.forUsername(admin.getUsername()).accountSummaries();
            assertEquals(2, groups.size());
            assertEquals(internalCount, groups.get(0).total());
            assertEquals(candidateCount, groups.get(1).total());
            groups.forEach(group -> assertEquals(group.total(), group.active() + group.inactive() + group.blocked()));
        });
        if (all.getTotalElements() > all.getSize()) {
            Page<AccountListService.AccountRow> next = accounts.findAccounts("", null, null, "", "name", 1);
            assertEquals(all.getTotalElements(), next.getTotalElements());
            assertTrue(next.stream().noneMatch(row ->
                    all.stream().anyMatch(firstPageRow -> firstPageRow.id().equals(row.id()))));
        }

        storedUsers.stream().filter(user -> RoleAuthorities.INTERNAL_ROLE_NAMES.contains(user.getRole().getRoleName())).findFirst().ifPresent(user -> {
            Page<AccountListService.AccountRow> byUsername = accounts.findAccounts(
                    user.getUsername(), null, null, "", "username", 0);
            assertFalse(byUsername.isEmpty());
            assertTrue(byUsername.stream().anyMatch(row -> user.getUserId().equals(row.id())));
            assertFalse(accounts.findAccounts(user.getEmail(), null, null, "", "name", 0).isEmpty());

            Page<AccountListService.AccountRow> byRoleAndStatus = accounts.findAccounts(
                    "", user.getRole().getRoleId(), null, user.getAccountStatus(), "newest", 0);
            assertFalse(byRoleAndStatus.isEmpty());
            assertTrue(byRoleAndStatus.stream().allMatch(row ->
                    user.getRole().getRoleName().equals(row.roleName())
                            && user.getAccountStatus().equals(row.status())));

            if (user.getDepartment() != null) {
                Page<AccountListService.AccountRow> byDepartment = accounts.findAccounts(
                        "", null, user.getDepartment().getDepartmentId(), "", "name", 0);
                assertTrue(byDepartment.stream().allMatch(row ->
                        user.getDepartment().getDepartmentName().equals(row.departmentName())));
            }
        });
        storedUsers.stream().filter(user -> "Candidate".equals(user.getRole().getRoleName())).findFirst().ifPresent(user -> {
            assertTrue(accounts.findAccounts(user.getUsername(), user.getRole().getRoleId(), null, "", "name", 0).isEmpty());
            assertTrue(accounts.findCandidateAccounts(user.getUsername(), "", "username", 0).stream().anyMatch(row -> row.id().equals(user.getUserId())));
            var profile = accounts.findCandidateAccounts(user.getEmail(), "", "name", 0);
            assertTrue(profile.stream().anyMatch(row -> row.id().equals(user.getUserId())));
        });
        System.out.println("Account separation SELECT-only: internal=" + internalCount + ", candidate=" + candidateCount);
        long historicalInternalProfiles = candidates.findAll().stream().filter(profile -> RoleAuthorities.INTERNAL_ROLE_NAMES.contains(profile.getAccount().getRole().getRoleName())).count();
        long candidateDepartments = storedUsers.stream().filter(user -> "Candidate".equals(user.getRole().getRoleName()) && user.getDepartment() != null).count();
        System.out.println("Account separation legacy audit SELECT-only: internal-linked-profiles=" + historicalInternalProfiles + ", candidate-with-department=" + candidateDepartments);
    }
}
