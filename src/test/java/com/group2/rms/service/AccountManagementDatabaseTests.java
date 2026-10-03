package com.group2.rms.service;

import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.candidate.repository.CandidateRepository;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.service.AccountManagementService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class AccountManagementDatabaseTests {

    @Autowired private AccountManagementService accounts;
    @Autowired private UserRepository users;
    @Autowired private CandidateRepository candidates;
    @Autowired private RoleRepository roles;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    @Test
    void candidateAccountAndProfilePersistWithContactDataOnUser() {
        Role candidateRole = roles.findAll().stream()
                .filter(role -> "Candidate".equals(role.getRoleName()))
                .findFirst().orElseThrow();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String username = "testcand" + suffix;
        String firstEmail = username + "@example.test";

        int userId = accounts.create(new AccountManagementService.CreateCommand(
                "Candidate Integration Test", username, firstEmail, null,
                candidateRole.getRoleId(), null, "validPassword12"));
        entityManager.flush();
        entityManager.clear();

        User user = users.findById(userId).orElseThrow();
        Candidate profile = candidates.findByAccountUserId(userId).orElseThrow();
        String originalHash = user.getPasswordHash();
        assertNotEquals("validPassword12", originalHash);
        assertTrue(passwordEncoder.matches("validPassword12", originalHash));
        assertEquals("Active", user.getAccountStatus());
        assertNull(user.getPhoneNumber());
        assertEquals(firstEmail, profile.getAccount().getEmail());

        String secondEmail = "updated" + suffix + "@example.test";
        accounts.update(userId, new AccountManagementService.UpdateCommand(
                "Candidate Integration Test", secondEmail, "0900000000",
                candidateRole.getRoleId(), null, "Inactive"));
        entityManager.flush();
        entityManager.clear();

        User updatedUser = users.findById(userId).orElseThrow();
        Candidate updatedProfile = candidates.findByAccountUserId(userId).orElseThrow();
        assertEquals(username, updatedUser.getUsername());
        assertEquals(originalHash, updatedUser.getPasswordHash());
        assertEquals(secondEmail, updatedUser.getEmail());
        assertEquals(secondEmail, updatedProfile.getAccount().getEmail());
        assertEquals("Inactive", updatedUser.getAccountStatus());
    }
}
