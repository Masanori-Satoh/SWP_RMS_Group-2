package com.group2.rms.service;

import com.group2.rms.repository.UserRepository;
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

    @Test
    void listUsesUserAccountsAndFiltersStoredFields() {
        Page<AccountListService.AccountRow> all = accounts.findAccounts("", null, null, "", "name", 0);
        assertEquals(users.count(), all.getTotalElements());
        assertTrue(accounts.findRoles().stream().anyMatch(role -> "Candidate".equals(role.getRoleName())));
        if (all.getTotalElements() > all.getSize()) {
            Page<AccountListService.AccountRow> next = accounts.findAccounts("", null, null, "", "name", 1);
            assertEquals(all.getTotalElements(), next.getTotalElements());
            assertTrue(next.stream().noneMatch(row ->
                    all.stream().anyMatch(firstPageRow -> firstPageRow.id().equals(row.id()))));
        }

        users.findAll().stream().findFirst().ifPresent(user -> {
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
    }
}
