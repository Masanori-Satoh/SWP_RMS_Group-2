package com.group2.rms.auth.service;

import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import com.group2.rms.auth.exception.PasswordRecoveryFlowException;
import com.group2.rms.auth.exception.PasswordRecoveryFlowException.Step;
import com.group2.rms.auth.exception.PasswordRecoveryUnavailableException;

import jakarta.servlet.http.HttpSession;

@Service
public class PasswordRecoveryService {
    private static final String RESET_STATE = PasswordRecoveryService.class.getName() + ".RESET_STATE";

    private static final Duration OTP_LIFETIME = Duration.ofMinutes(15);
    private static final Duration RESEND_DELAY = Duration.ofMinutes(5);
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final PasswordResetService resetService;
    private final PasswordResetEmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    private final SecureRandom random = new SecureRandom();

    private final ConcurrentMap<Integer, SendWindow> sendWindows = new ConcurrentHashMap<>();

    @Autowired
    public PasswordRecoveryService(PasswordResetService resetService,PasswordResetEmailSender emailSender,PasswordEncoder passwordEncoder) {
        this(resetService,emailSender, passwordEncoder, Clock.systemUTC());
    }

    public PasswordRecoveryService(PasswordResetService resetService, PasswordResetEmailSender emailSender, PasswordEncoder passwordEncoder, Clock clock) {
        this.resetService = resetService;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public void requestReset(String email, HttpSession session) {
        //check if mail sender be configured
        if (!emailSender.isConfigured()) {
            throw new PasswordRecoveryUnavailableException();
        }
        var account = resetService.findActiveAccount(email);

        synchronized (WebUtils.getSessionMutex(session)) {
            Instant now = clock.instant();
            clearExpiredSendWindows(now);

            SendWindow reservation = new SendWindow(now.plus(RESEND_DELAY), true);

            // compute() kiểm tra và giữ chỗ gửi một cách atomic.
            sendWindows.compute(account.userId(), (userId, current) -> {

                if (current != null && current.sending()) {
                    throw new PasswordRecoveryFlowException(
                            Step.FORGOT,
                            "email",
                            "RESET_EMAIL_IN_PROGRESS",
                            "A reset email is already being sent. Please wait.");
                }

                if (current != null
                        && now.isBefore(current.nextAllowedAt())) {

                    long remaining = secondsUntil(current.nextAllowedAt(), now);

                    throw new PasswordRecoveryFlowException(
                            Step.FORGOT,
                            "email",
                            "RESET_EMAIL_COOLDOWN",
                            "Please wait before requesting another code.",
                            remaining);
                }

                return reservation;
            });

            boolean mailSent = false;

            try {
                String otp = generateOtp();

                // Session chỉ lưu hash OTP.
                String otpHash = passwordEncoder.encode(otp);

                emailSender.sendOtp(account.email(), otp);
                mailSent = true;

                Instant sentAt = clock.instant();

                sendWindows.replace(
                        account.userId(),
                        reservation,
                        new SendWindow(sentAt.plus(RESEND_DELAY), false));

                // Thay yêu cầu cũ của session bằng yêu cầu mới.
                session.setAttribute(
                        RESET_STATE,
                        new ResetState(
                                account,
                                otpHash,
                                sentAt.plus(OTP_LIFETIME)));

            } finally {
                // Gửi thất bại thì trả lại quyền gửi.
                // Exception vẫn được ném lên GlobalExceptionHandler.
                if (!mailSent) {
                    sendWindows.remove(account.userId(), reservation);
                }
            }
        }
    }

    public boolean hasPendingReset(HttpSession session) {

        synchronized (WebUtils.getSessionMutex(session)) {
            ResetState state = liveState(session);

            return state != null && !state.verified;
        }
    }

    public void verifyOtp(String otp, HttpSession session) {

        synchronized (WebUtils.getSessionMutex(session)) {
            ResetState state = requireLiveState(session);

            if (state.verified) {
                return;
            }

            boolean valid = otp != null
                    && otp.matches("[0-9]{6}")
                    && passwordEncoder.matches(otp, state.otpHash);

            if (!valid) {
                state.failedAttempts++;

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

            // Chỉ Service tạo trạng thái này sau khi kiểm tra OTP.
            state.verified = true;

            // OTP đã dùng; không cần giữ hash OTP nữa.
            state.otpHash = null;
        }
    }

    public boolean canResetPassword(HttpSession session) {

        synchronized (WebUtils.getSessionMutex(session)) {
            ResetState state = liveState(session);

            return state != null && state.verified;
        }
    }

    public void completeReset(
            String newPassword,
            HttpSession session) {

        synchronized (WebUtils.getSessionMutex(session)) {
            ResetState state = requireLiveState(session);

            if (!state.verified) {
                throw new PasswordRecoveryFlowException(
                        Step.OTP,
                        "otp",
                        "OTP_REQUIRED",
                        "Verify your email code before resetting your password.");
            }

            // Service này có transaction và kiểm tra lại tài khoản.
            resetService.changePassword(
                    state.account,
                    newPassword,
                    state.expiresAt);

            // changePassword() qua Spring proxy đã commit thành công.
            session.removeAttribute(RESET_STATE);
        }
    }

    public String getPendingResetEmail(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            return requireLiveState(session).account.email();
        }
    }

    public long getResendRemainingSeconds(HttpSession session) {

        synchronized (WebUtils.getSessionMutex(session)) {
            Object value = session.getAttribute(RESET_STATE);

            if (!(value instanceof ResetState state)) {
                return 0;
            }

            SendWindow window = sendWindows.get(state.account.userId());

            if (window == null) {
                return 0;
            }

            return window.sending()
                    ? RESEND_DELAY.toSeconds()
                    : secondsUntil(
                            window.nextAllowedAt(),
                            clock.instant());
        }
    }

    private String generateOtp() {
        return String.format(
                Locale.ROOT,
                "%06d",
                random.nextInt(1_000_000));
    }

    private ResetState liveState(HttpSession session) {
        Object value = session.getAttribute(RESET_STATE);

        if (!(value instanceof ResetState state)) {
            return null;
        }

        if (!clock.instant().isBefore(state.expiresAt)
                || !resetService.isCurrent(state.account)) {

            session.removeAttribute(RESET_STATE);
            return null;
        }

        return state;
    }

    private ResetState requireLiveState(HttpSession session) {
        ResetState state = liveState(session);

        if (state == null) {
            throw new PasswordRecoveryFlowException(
                    Step.FORGOT,
                    null,
                    "RESET_REQUEST_INVALID",
                    "Your reset request is invalid or expired. Request a new code.");
        }

        return state;
    }

    private void clearExpiredSendWindows(Instant now) {
        sendWindows.forEach((userId, window) -> {
            if (!window.sending()
                    && !now.isBefore(window.nextAllowedAt())) {
                sendWindows.remove(userId, window);
            }
        });
    }

    private static long secondsUntil(
            Instant deadline,
            Instant now) {

        long millis = Duration.between(now, deadline).toMillis();

        return millis <= 0 ? 0 : (millis + 999) / 1_000;
    }

    private record SendWindow(
            Instant nextAllowedAt,
            boolean sending) {
    }

    private static final class ResetState implements Serializable {

        private static final long serialVersionUID = 1L;

        private final PasswordResetService.AccountSnapshot account;
        private final Instant expiresAt;

        private String otpHash;
        private int failedAttempts;
        private boolean verified;

        private ResetState(
                PasswordResetService.AccountSnapshot account,
                String otpHash,
                Instant expiresAt) {

            this.account = account;
            this.otpHash = otpHash;
            this.expiresAt = expiresAt;
        }
    }
}
