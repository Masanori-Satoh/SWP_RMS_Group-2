package com.group2.rms.auth.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.group2.rms.auth.exception.PasswordRecoveryFlowException;
import com.group2.rms.auth.exception.PasswordRecoveryFlowException.Step;
import com.group2.rms.auth.exception.PasswordRecoveryUnavailableException;
import com.group2.rms.auth.dto.AccountSnapshotResponse;

import jakarta.servlet.http.HttpSession;

@Service
public class PasswordRecoveryService {
    private static final String RESET_STATE = PasswordRecoveryService.class.getName() + ".RESET_STATE";

    private static final Duration OTP_LIFETIME = Duration.ofMinutes(1);
    private static final Duration RESEND_DELAY = Duration.ofMinutes(1);
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final PasswordResetService resetService;
    private final PasswordResetEmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    private final SecureRandom random = new SecureRandom();

    private final Map<Integer, Instant> sendCooldowns = new ConcurrentHashMap<>();

    @Autowired
    public PasswordRecoveryService(PasswordResetService resetService, PasswordResetEmailSender emailSender,
            PasswordEncoder passwordEncoder) {
        this(resetService, emailSender, passwordEncoder, Clock.systemUTC());
    }

    public PasswordRecoveryService(PasswordResetService resetService, PasswordResetEmailSender emailSender,
            PasswordEncoder passwordEncoder, Clock clock) {
        this.resetService = resetService;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public void requestReset(String email, HttpSession session) {
        // check if mail sender be configured
        if (!emailSender.isConfigured()) {
            throw new PasswordRecoveryUnavailableException();
        }
        AccountSnapshotResponse account = resetService.findActiveAccount(email);
        // get time now
        Instant now = clock.instant();
        // revmove if time reset is expired
        sendCooldowns.values().removeIf(time -> !now.isBefore(time));
        // get time next allowed
        Instant nextAllowed = sendCooldowns.get(account.userId());

        // check if otp is sending
        if (nextAllowed != null && now.isBefore(nextAllowed)) {
            throw new PasswordRecoveryFlowException(Step.FORGOT, "email", "RESET_EMAIL_COOLDOWN",
                    "Please wait before requesting another code.", secondsUntil(nextAllowed, now));
        }

        // set time to send in next time
        Instant reservation = now.plus(RESEND_DELAY);
        sendCooldowns.put(account.userId(), reservation);

        boolean mailSent = false;

        try {
            String otp = String.format("%06d", random.nextInt(1000000));

            // store hash otp
            String otpHash = passwordEncoder.encode(otp);

            emailSender.sendOtp(account.email(), otp);
            mailSent = true;

            Instant sentAt = clock.instant();

            sendCooldowns.replace(account.userId(), reservation, sentAt.plus(RESEND_DELAY));

            // update new session
            session.setAttribute(RESET_STATE, new ResetState(account, otpHash, sentAt.plus(OTP_LIFETIME)));

        } finally {
            // check if not send
            if (!mailSent) {
                sendCooldowns.remove(account.userId(), reservation);
            }
        }

    }

    public boolean hasPendingReset(HttpSession session) {
        ResetState state = liveState(session);
        return state != null && !state.verified;
    }

    public void verifyOtp(String otp, HttpSession session) {
        ResetState state = requireLiveState(session);
        // check if state is verified
        if (state.verified) {
            return;
        }
        // check if otp is not null match patterr and match with otp of system
        boolean valid = otp != null && otp.matches("[0-9]{6}") && passwordEncoder.matches(otp, state.otpHash);
        // check if otp is not valid
        if (!valid) {
            state.failedAttempts++;
            // check if failed attempts is more than max failed attempts
            if (state.failedAttempts >= MAX_FAILED_ATTEMPTS) {
                session.removeAttribute(RESET_STATE);

                throw new PasswordRecoveryFlowException(
                        Step.FORGOT,
                        null,
                        "OTP_ATTEMPTS_EXCEEDED",
                        "Too many incorrect attempts. Request a new code.");
            }

            throw new PasswordRecoveryFlowException(
                    Step.OTP,
                    "otp",
                    "OTP_INVALID",
                    "The verification code is incorrect.");
        }

        state.verified = true;
        state.failedAttempts = 0;
    }

    public boolean canResetPassword(HttpSession session) {
        ResetState state = liveState(session);
        // check if state not null and verified
        return state != null && state.verified;
    }

    public void completeReset(String newPassword, HttpSession session) {

        ResetState state = requireLiveState(session);
        // check if state is not verified
        if (!state.verified) {
            throw new PasswordRecoveryFlowException(
                    Step.OTP,
                    "otp",
                    "OTP_REQUIRED",
                    "Verify your email code before resetting your password.");
        }

        resetService.changePassword(state.account, newPassword, state.expiresAt);

        // changePassword() qua Spring proxy đã commit thành công.
        session.removeAttribute(RESET_STATE);

    }

    public String getPendingResetEmail(HttpSession session) {
        return requireLiveState(session).account.email();
    }

    public long getResendRemainingSeconds(HttpSession session) {

        Object value = session.getAttribute(RESET_STATE);
        // check if value is not ResetState
        if (!(value instanceof ResetState state)) {
            return 0;
        }

        Instant nextAllowed = sendCooldowns.get(state.account.userId());
        // check if not send otp
        if (nextAllowed == null) {
            return 0;
        }

        return secondsUntil(nextAllowed, clock.instant());

    }

    private ResetState liveState(HttpSession session) {
        Object value = session.getAttribute(RESET_STATE);
        // check if value is null or not resetState
        if (!(value instanceof ResetState state)) {
            return null;
        }
        // check if state is not expired or account is change
        if (!clock.instant().isBefore(state.expiresAt) || !resetService.isCurrent(state.account)) {
            session.removeAttribute(RESET_STATE);
            return null;
        }

        return state;
    }

    private ResetState requireLiveState(HttpSession session) {
        ResetState state = liveState(session);
        // check if account is change or state is changed
        if (state == null) {
            throw new PasswordRecoveryFlowException(Step.FORGOT, null, "RESET_REQUEST_INVALID",
                    "Your reset request is invalid or expired. Request a new code.");
        }

        return state;
    }

    private static long secondsUntil(Instant deadline, Instant now) {
        long millis = Duration.between(now, deadline).toMillis();
        return millis <= 0 ? 0 : (millis + 999) / 1_000;
    }

    private static final class ResetState {

        private final AccountSnapshotResponse account;
        private final Instant expiresAt;

        private String otpHash;
        private int failedAttempts;
        private boolean verified;

        private ResetState(
                AccountSnapshotResponse account,
                String otpHash,
                Instant expiresAt) {

            this.account = account;
            this.otpHash = otpHash;
            this.expiresAt = expiresAt;
        }
    }
}
