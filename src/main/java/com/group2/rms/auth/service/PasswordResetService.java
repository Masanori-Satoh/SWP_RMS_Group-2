package com.group2.rms.auth.service;
import com.group2.rms.auth.dto.AccountSnapshotResponse;

import com.group2.rms.auth.exception.PasswordRecoveryFlowException;
import com.group2.rms.auth.exception.PasswordRecoveryFlowException.Step;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.service.UserAuditLogger;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

import java.util.Objects;

/**
 * A short-lived signed email link, bound to the current account credentials. No
 * JWT or token table.
 */
@Service
public class PasswordResetService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final UserAuditLogger audit;

    @Autowired
    public PasswordResetService(
            UserRepository users,
            PasswordEncoder passwordEncoder, UserAuditLogger audit) {
        this(users, passwordEncoder, Clock.systemUTC(), audit);
    }

    public PasswordResetService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            Clock clock, UserAuditLogger audit) {

        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public AccountSnapshotResponse  findActiveAccount(String email) {
        // check if email is not valid
        if (email == null || email.isBlank()) {
            throw new PasswordRecoveryFlowException(
                    Step.FORGOT,
                    "email",
                    "EMAIL_REQUIRED",
                    "Enter an email address.");
        }

        User user = users.findByEmailIgnoreCase(email.trim())
                // check if email is not found
                .orElseThrow(() -> new PasswordRecoveryFlowException(
                        Step.FORGOT,
                        "email",
                        "EMAIL_NOT_FOUND",
                        "No account exists with this email address."));
        // check if account is not active
        if (!"Active".equals(user.getAccountStatus())) {
            throw new PasswordRecoveryFlowException(
                    Step.FORGOT,
                    "email",
                    "ACCOUNT_NOT_ACTIVE",
                    "This account is not active.");
        }

        return new AccountSnapshotResponse (user.getUserId(), user.getEmail(), user.getPasswordHash());
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public boolean isCurrent(AccountSnapshotResponse  expected) {
        // check if account is null
        if (expected == null) {
            return false;
        }
        // check if account is current
        return users.findById(expected.userId())
                .filter(user -> matchesAccount(user, expected))
                .isPresent();
    }

    @Transactional
    public void changePassword(AccountSnapshotResponse  expected, String newPassword, Instant expiresAt) {

        // check if account is not exist or expiresAt is null  
        if (expected == null || expiresAt == null) {
            throw invalidRequest();
        }
        //check if new password is not valid
        if (newPassword == null|| newPassword.isBlank()|| newPassword.length() < 8|| newPassword.length() > 32) {

            throw new PasswordRecoveryFlowException(
                    Step.PASSWORD,
                    "password",
                    "PASSWORD_INVALID",
                    "Password must contain 8–32 characters.");
        }

        User user = users.findByIdForUpdate(expected.userId())
                .orElseThrow(() -> PasswordResetService.invalidRequest());

        
        if (!clock.instant().isBefore(expiresAt)|| !matchesAccount(user, expected)) {
            throw invalidRequest();
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.saveAndFlush(user);
        audit.record(user, "UPDATE", "User", user.getUserId(), null, "Đặt lại mật khẩu thành công");
    }

    private static boolean matchesAccount(User user, AccountSnapshotResponse  expected) {
        // check if account is active email and password is not change
        return "Active".equals(user.getAccountStatus())
                && Objects.equals(user.getEmail(), expected.email())
                && Objects.equals(user.getPasswordHash(), expected.passwordHash());
    }

    private static PasswordRecoveryFlowException invalidRequest() {
        return new PasswordRecoveryFlowException(
                Step.FORGOT,
                null,
                "RESET_REQUEST_INVALID",
                "Your reset request is invalid or expired. Request a new code.");
    }

  
}
