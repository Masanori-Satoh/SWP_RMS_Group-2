package com.group2.rms.auth.service;

import com.group2.rms.auth.exception.RegistrationFlowException;
import com.group2.rms.auth.exception.RegistrationFlowException.RegistrationDetails;
import com.group2.rms.auth.exception.RegistrationFlowException.Step;
import com.group2.rms.auth.service.CandidateRegistrationService.PreparedRegistration;
import com.group2.rms.auth.service.CandidateRegistrationService.RegisterCommand;
import com.group2.rms.user.exception.AccountFieldException;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

@Service
public class RegistrationVerificationService {
    private static final String REGISTER_STATE = RegistrationVerificationService.class.getName() + ".REGISTER_STATE";
    private static final Duration OTP_LIFETIME = Duration.ofMinutes(15);
    private static final Duration RESEND_DELAY = Duration.ofMinutes(5);
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final CandidateRegistrationService registration;
    private final RegistrationEmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentMap<String, SendWindow> sendWindows = new ConcurrentHashMap<>();

    @Autowired
    public RegistrationVerificationService(CandidateRegistrationService registration,
            RegistrationEmailSender emailSender, PasswordEncoder passwordEncoder) {
        this(registration, emailSender, passwordEncoder, Clock.systemUTC());
    }

    public RegistrationVerificationService(CandidateRegistrationService registration,
            RegistrationEmailSender emailSender, PasswordEncoder passwordEncoder, Clock clock) {
        this.registration = registration;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public void requestRegistration(RegisterCommand command, HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            PreparedRegistration prepared;
            try {
                prepared = registration.prepare(command);
            } catch (AccountFieldException exception) {
                throw new RegistrationFlowException(Step.REGISTER, exception.getField(),
                        "REGISTER_FIELD_INVALID", exception.getMessage(), 0,
                        new RegistrationDetails(command.fullName(), command.username(), command.email()));
            }
            sendCode(prepared, session, Step.REGISTER);
        }
    }

    public boolean hasPendingRegistration(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            Object value = session.getAttribute(REGISTER_STATE);
            if (!(value instanceof PendingRegistration pending)) return false;
            if (!clock.instant().isBefore(pending.expiresAt)) {
                session.removeAttribute(REGISTER_STATE);
                return false;
            }
            return true;
        }
    }

    public String getPendingRegistrationEmail(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            return requirePending(session).registration.email();
        }
    }

    public long getResendRemainingSeconds(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            PendingRegistration pending = requirePending(session);
            SendWindow window = sendWindows.get(emailKey(pending.registration.email()));
            return window == null ? 0 : window.sending()
                    ? RESEND_DELAY.toSeconds() : secondsUntil(window.nextAllowedAt(), clock.instant());
        }
    }

    public void resendOtp(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            PendingRegistration pending = requirePending(session);
            sendCode(pending.registration, session, Step.OTP);
        }
    }

    public int verifyAndRegister(String otp, HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            PendingRegistration pending = requirePending(session);
            if (otp == null || !otp.matches("[0-9]{6}") || !passwordEncoder.matches(otp, pending.otpHash)) {
                if (++pending.failedAttempts >= MAX_FAILED_ATTEMPTS) {
                    session.removeAttribute(REGISTER_STATE);
                    throw failure(Step.REGISTER, null, "REGISTER_OTP_ATTEMPTS_EXCEEDED",
                            "Too many incorrect attempts. Register again.", 0, pending.registration);
                }
                throw failure(Step.OTP, "otp", "REGISTER_OTP_INVALID",
                        "The verification code is incorrect.", 0, pending.registration);
            }
            int userId;
            try {
                // The separate transactional bean has committed before it returns.
                userId = registration.registerVerified(pending.registration);
            } catch (AccountFieldException exception) {
                session.removeAttribute(REGISTER_STATE);
                throw failure(Step.REGISTER, exception.getField(), "REGISTER_FIELD_INVALID",
                        exception.getMessage(), 0, pending.registration);
            } catch (DataIntegrityViolationException exception) {
                if (!isLoginIdentifierConflict(exception)) throw exception;
                session.removeAttribute(REGISTER_STATE);
                throw failure(Step.REGISTER, null, "REGISTER_ACCOUNT_CONFLICT",
                        "The username or email is already in use.", 0, pending.registration);
            }
            session.removeAttribute(REGISTER_STATE);
            return userId;
        }
    }

    private void sendCode(PreparedRegistration prepared, HttpSession session, Step step) {
        Instant now = clock.instant();
        sendWindows.forEach((key, window) -> {
            if (!window.sending() && !now.isBefore(window.nextAllowedAt())) sendWindows.remove(key, window);
        });
        String key = emailKey(prepared.email());
        SendWindow reservation = new SendWindow(now.plus(RESEND_DELAY), true);
        sendWindows.compute(key, (email, current) -> {
            if (current != null && current.sending()) {
                throw failure(step, step == Step.REGISTER ? "email" : null, "REGISTER_EMAIL_IN_PROGRESS",
                        "A registration email is already being sent. Please wait.",
                        RESEND_DELAY.toSeconds(), prepared);
            }
            if (current != null && now.isBefore(current.nextAllowedAt())) {
                throw failure(step, step == Step.REGISTER ? "email" : null, "REGISTER_EMAIL_COOLDOWN",
                        "Please wait before requesting another code.", secondsUntil(current.nextAllowedAt(), now), prepared);
            }
            return reservation;
        });
        boolean sent = false;
        try {
            String otp = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
            String otpHash = passwordEncoder.encode(otp);
            emailSender.sendOtp(prepared.email(), otp);
            sent = true;
            Instant sentAt = clock.instant();
            sendWindows.replace(key, reservation, new SendWindow(sentAt.plus(RESEND_DELAY), false));
            session.setAttribute(REGISTER_STATE,
                    new PendingRegistration(prepared, otpHash, sentAt.plus(OTP_LIFETIME)));
        } catch (RegistrationFlowException exception) {
            throw failure(step, exception.getField(), exception.getErrorCode(), exception.getMessage(),
                    exception.getRetryAfterSeconds(), prepared);
        } finally {
            if (!sent) sendWindows.remove(key, reservation);
        }
    }

    private PendingRegistration requirePending(HttpSession session) {
        Object value = session.getAttribute(REGISTER_STATE);
        if (value instanceof PendingRegistration pending) {
            if (clock.instant().isBefore(pending.expiresAt)) return pending;
            session.removeAttribute(REGISTER_STATE);
            throw failure(Step.REGISTER, null, "REGISTER_REQUEST_INVALID",
                    "Your registration request is invalid or expired. Register again.", 0, pending.registration);
        }
        throw new RegistrationFlowException(Step.REGISTER, null, "REGISTER_REQUEST_INVALID",
                "Your registration request is invalid or expired. Register again.");
    }

    private static RegistrationFlowException failure(Step step, String field, String code,
            String message, long retryAfter, PreparedRegistration prepared) {
        return new RegistrationFlowException(step, field, code, message, retryAfter,
                new RegistrationDetails(prepared.fullName(), prepared.username(), prepared.email()));
    }

    private static boolean isLoginIdentifierConflict(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            String name = cause instanceof org.hibernate.exception.ConstraintViolationException constraint
                    ? constraint.getConstraintName() : cause.getMessage();
            if (name != null) {
                String normalized = name.toLowerCase(Locale.ROOT);
                if (normalized.contains("uq_user_username") || normalized.contains("uq_user_email")) return true;
            }
        }
        return false;
    }

    private static String emailKey(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static long secondsUntil(Instant deadline, Instant now) {
        long millis = Duration.between(now, deadline).toMillis();
        return millis <= 0 ? 0 : (millis + 999) / 1000;
    }

    private record SendWindow(Instant nextAllowedAt, boolean sending) { }

    private static final class PendingRegistration implements Serializable {
        private static final long serialVersionUID = 1L;
        private final PreparedRegistration registration;
        private final String otpHash;
        private final Instant expiresAt;
        private int failedAttempts;

        private PendingRegistration(PreparedRegistration registration, String otpHash, Instant expiresAt) {
            this.registration = registration;
            this.otpHash = otpHash;
            this.expiresAt = expiresAt;
        }

        @Override
        public String toString() { return "PendingRegistration[redacted]"; }
    }
}
