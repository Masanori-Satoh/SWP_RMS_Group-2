package com.group2.rms.auth.service;

import com.group2.rms.auth.exception.InvalidResetTokenException;
import com.group2.rms.auth.exception.PasswordRecoveryFlowException;
import com.group2.rms.auth.exception.PasswordRecoveryFlowException.Step;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.exception.AccountFieldException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.group2.rms.auth.exception.InvalidResetTokenException;

/**
 * A short-lived signed email link, bound to the current account credentials. No
 * JWT or token table.
 */
@Service
public class PasswordResetService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Autowired
    public PasswordResetService(
            UserRepository users,
            PasswordEncoder passwordEncoder) {
        this(users, passwordEncoder, Clock.systemUTC());
    }

    public PasswordResetService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            Clock clock) {

        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AccountSnapshot findActiveAccount(String email) {
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

        return new AccountSnapshot(
                user.getUserId(),
                user.getEmail(),
                user.getPasswordHash());
    }

    @Transactional(readOnly = true)
    public boolean isCurrent(AccountSnapshot expected) {

        if (expected == null) {
            return false;
        }

        return users.findById(expected.userId())
                .filter(user -> matchesAccount(user, expected))
                .isPresent();
    }

    @Transactional
    public void changePassword(
            AccountSnapshot expected,
            String newPassword,
            Instant expiresAt) {

        if (expected == null || expiresAt == null) {
            throw invalidRequest();
        }

        if (newPassword == null
                || newPassword.isBlank()
                || newPassword.length() < 8
                || newPassword.length() > 32) {

            throw new PasswordRecoveryFlowException(
                    Step.PASSWORD,
                    "password",
                    "PASSWORD_INVALID",
                    "Password must contain 8–32 characters.");
        }

        User user = users.findByIdForUpdate(expected.userId())
                .orElseThrow(PasswordResetService::invalidRequest);

        // Kiểm tra lại sau khi đã lấy khóa ghi của User.
        if (!clock.instant().isBefore(expiresAt)
                || !matchesAccount(user, expected)) {
            throw invalidRequest();
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.saveAndFlush(user);
    }

    private static boolean matchesAccount(
            User user,
            AccountSnapshot expected) {

        return "Active".equals(user.getAccountStatus())
                && Objects.equals(user.getEmail(), expected.email())
                && Objects.equals(
                        user.getPasswordHash(),
                        expected.passwordHash());
    }

    private static PasswordRecoveryFlowException invalidRequest() {
        return new PasswordRecoveryFlowException(
                Step.FORGOT,
                null,
                "RESET_REQUEST_INVALID",
                "Your reset request is invalid or expired. Request a new code.");
    }

    // Snapshot nội bộ giữa các Service; không đưa vào Model/JSON/log.
    record AccountSnapshot(
            Integer userId,
            String email,
            String passwordHash) implements Serializable {

        @Override
        public String toString() {
            return "AccountSnapshot[redacted]";
        }
    }
}
