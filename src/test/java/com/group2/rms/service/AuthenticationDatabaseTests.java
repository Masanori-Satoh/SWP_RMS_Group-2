package com.group2.rms.service;

import com.group2.rms.auth.exception.InvalidResetTokenException;
import com.group2.rms.auth.service.CandidateRegistrationService;
import com.group2.rms.auth.service.PasswordResetService;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.user.entity.User;
import com.group2.rms.candidate.repository.CandidateRepository;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.password.reset.secret=integration-test-signing-key-longer-than-32-bytes")
@Transactional
@Rollback
class AuthenticationDatabaseTests {
    private static final String TEST_SECRET = "integration-test-signing-key-longer-than-32-bytes";

    @Autowired private CandidateRegistrationService registration;
    @Autowired private PasswordResetService reset;
    @Autowired private UserRepository users;
    @Autowired private CandidateRepository candidates;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    @Test
    void publicRegistrationCreatesActiveCandidateAccountAndProfile() {
        String username = uniqueUsername();
        registration.register(new CandidateRegistrationService.RegisterCommand(
                "Ứng viên đăng ký", username, username + "@example.test", "oldPassword12"));
        entityManager.flush();
        entityManager.clear();

        User user = users.findByUsernameIgnoreCase(username).orElseThrow();
        Candidate profile = candidates.findByAccountUserId(user.getUserId()).orElseThrow();
        assertEquals("Candidate", user.getRole().getRoleName());
        assertEquals("Active", user.getAccountStatus());
        assertEquals(user.getUserId(), profile.getAccount().getUserId());
        assertNull(user.getDepartment());
        assertNull(user.getPhoneNumber());
        assertNotEquals("oldPassword12", user.getPasswordHash());
        assertTrue(passwordEncoder.matches("oldPassword12", user.getPasswordHash()));
    }

    @Test
    void signedLinkChangesPasswordOnceWithoutTokenTable() {
        User user = registeredCandidate();
        PasswordResetService.ResetLink link = reset.request(user.getEmail()).orElseThrow();
        assertTrue(link.token().startsWith("v1."));
        assertFalse(link.token().contains(user.getEmail()));
        assertFalse(link.token().contains(user.getPasswordHash()));
        assertTrue(reset.isValid(link.token()));

        reset.reset(link.token(), link.otp(), "newPassword12");

        entityManager.flush();
        entityManager.clear();
        User updated = users.findById(user.getUserId()).orElseThrow();
        assertTrue(passwordEncoder.matches("newPassword12", updated.getPasswordHash()));
        assertFalse(passwordEncoder.matches("oldPassword12", updated.getPasswordHash()));
        assertThrows(InvalidResetTokenException.class, () -> reset.reset(link.token(), link.otp(), "anotherPassword12"));
    }

    @Test
    void expiredTamperedOrInactiveAccountCannotReset() {
        User user = registeredCandidate();
        PasswordResetService.ResetLink link = reset.request(user.getEmail()).orElseThrow();
        PasswordResetService future = new PasswordResetService(users, passwordEncoder,
                TEST_SECRET, Clock.offset(Clock.systemUTC(), Duration.ofMinutes(16)));
        assertFalse(future.isValid(link.token()));
        assertThrows(InvalidResetTokenException.class, () -> future.reset(link.token(), link.otp(), "newPassword12"));
        assertFalse(reset.isValid(link.token() + "tampered"));

        user.setAccountStatus("Inactive");
        entityManager.flush();
        assertFalse(reset.isValid(link.token()));
        assertThrows(InvalidResetTokenException.class, () -> reset.reset(link.token(), link.otp(), "newPassword12"));
        assertTrue(reset.request(user.getEmail()).isEmpty());
    }

    private User registeredCandidate() {
        String username = uniqueUsername();
        registration.register(new CandidateRegistrationService.RegisterCommand(
                "Ứng viên đăng ký", username, username + "@example.test", "oldPassword12"));
        entityManager.flush();
        return users.findByUsernameIgnoreCase(username).orElseThrow();
    }

    private static String uniqueUsername() {
        return "candidate" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
