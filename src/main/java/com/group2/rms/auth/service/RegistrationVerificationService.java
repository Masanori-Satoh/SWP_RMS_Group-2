package com.group2.rms.auth.service;

import com.group2.rms.auth.exception.RegistrationFlowException;
import com.group2.rms.auth.exception.RegistrationFlowException.RegistrationDetails;
import com.group2.rms.auth.exception.RegistrationFlowException.Step;
import com.group2.rms.auth.dto.RegisterAccountRequest;
import com.group2.rms.user.service.AccountManagementService;
import com.group2.rms.user.exception.AccountFieldException;
import jakarta.servlet.http.HttpSession;
import com.group2.rms.auth.dto.PreparedRegistrationResponse;

import java.security.SecureRandom;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegistrationVerificationService {
    private static final String REGISTER_STATE = RegistrationVerificationService.class.getName() + ".REGISTER_STATE";
    private static final Duration OTP_LIFETIME = Duration.ofMinutes(1);
    private static final Duration RESEND_DELAY = Duration.ofMinutes(1);
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final AccountManagementService accountManagementService;
    private final RegistrationEmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Instant> sendCooldowns = new ConcurrentHashMap<>();

    public RegistrationVerificationService(AccountManagementService accountManagementService,
            RegistrationEmailSender emailSender, PasswordEncoder passwordEncoder) {
        this.accountManagementService = accountManagementService;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
    }

    public void requestRegistration(RegisterAccountRequest registerAccountRequest, HttpSession session) {

        String fullName = registerAccountRequest.getFullName().trim();
        String username = registerAccountRequest.getUsername().trim();
        String email = registerAccountRequest.getEmail().trim();

        try {
            accountManagementService.assertLoginIdentifiersAvailable(username, email);
        } catch (AccountFieldException e) {
            throw new RegistrationFlowException(Step.REGISTER, e.getField(), "REGISTER_FIELD_INVALID", e.getMessage(),
                    0, new RegistrationDetails(fullName, username, email));
        }
        PreparedRegistrationResponse prepared = new PreparedRegistrationResponse(fullName, username, email,
                passwordEncoder.encode(registerAccountRequest.getPassword()));
        sendCode(prepared, session, Step.REGISTER);

    }

    public String getPendingRegistrationEmail(HttpSession session) {

        return requirePending(session).registration.email();

    }

    public long getResendRemainingSeconds(HttpSession session) {

        PendingRegistration pending = requirePending(session);
        Instant nextAllowed = sendCooldowns.get(emailKey(pending.registration.email()));
        long remainingSeconds = 0;
        // check if remain time
        if (nextAllowed != null) {
            remainingSeconds = secondsUntil(nextAllowed, Instant.now());
        }
        return remainingSeconds;

    }

    public void resendOtp(HttpSession session) {

        PendingRegistration pending = requirePending(session);
        sendCode(pending.registration, session, Step.OTP);

    }

    public int verifyAndRegister(String otp, HttpSession session) {

        PendingRegistration pending = requirePending(session);
        // check if otp is not null and otp is 6 digits and otp is not matched
        if (otp == null || !otp.matches("[0-9]{6}") || !passwordEncoder.matches(otp, pending.otpHash)) {
            // check if exceed failed attemp
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
            userId = accountManagementService.createCandidateWithPasswordHash(
                    pending.registration.fullName(),
                    pending.registration.username(),
                    pending.registration.email(),
                    pending.registration.passwordHash());
        } catch (AccountFieldException exception) {
            session.removeAttribute(REGISTER_STATE);
            throw failure(Step.REGISTER, exception.getField(), "REGISTER_FIELD_INVALID",
                    exception.getMessage(), 0, pending.registration);
        } catch (DataIntegrityViolationException exception) {
            if (!isLoginIdentifierConflict(exception))
                throw exception;
            session.removeAttribute(REGISTER_STATE);
            throw failure(Step.REGISTER, null, "REGISTER_ACCOUNT_CONFLICT",
                    "The username or email is already in use.", 0, pending.registration);
        }
        session.removeAttribute(REGISTER_STATE);
        return userId;

    }

    private void sendCode(PreparedRegistrationResponse prepared, HttpSession session, Step step) {
        Instant now = Instant.now();
        String key = emailKey(prepared.email());
        sendCooldowns.values().removeIf(time -> !now.isBefore(time));
        Instant nextAllowed = sendCooldowns.get(key);

        String fieldError = null;
        // check if field error is email
        if (step == Step.REGISTER) {
            fieldError = "email";
        }
        // check if is sending
        if (nextAllowed != null && now.isBefore(nextAllowed)) {
            throw failure(step, fieldError, "REGISTER_EMAIL_COOLDOWN", "Vui lòng đợi trước khi gửi yêu cầu mới.",
                    secondsUntil(nextAllowed, now), prepared);
        }

        String otp = String.format("%06d", random.nextInt(1000000));

        String otpHash = passwordEncoder.encode(otp);

        try {
            emailSender.sendOtp(prepared.email(), otp);
        } catch (RegistrationFlowException exception) {
            throw failure(step, exception.getField(), exception.getErrorCode(), exception.getMessage(),
                    exception.getRetryAfterSeconds(), prepared);
        }

        Instant sendAt = Instant.now();
        sendCooldowns.put(key, sendAt.plus(RESEND_DELAY));
        session.setAttribute(REGISTER_STATE, new PendingRegistration(prepared, otpHash, sendAt.plus(OTP_LIFETIME)));

    }

    private PendingRegistration requirePending(HttpSession session) {
        Object value = session.getAttribute(REGISTER_STATE);
        // check if regist state is change type
        if (!(value instanceof PendingRegistration pending)) {
            throw new RegistrationFlowException(Step.REGISTER, null, "REGISTER_REQUEST_INVALID",
                    "Vui lòng đăng ký trước khi xác minh email.");
        }
        // check if otp is expired
        if (!Instant.now().isBefore(pending.expiresAt)) {
            session.removeAttribute(REGISTER_STATE);
            throw failure(Step.REGISTER, null, "REGISTER_REQUEST_INVALID",
                    "Yêu cầu đăng kí đã hết hạn. Vui lòng đăng kí lại", 0, pending.registration);
        }

        return pending;
    }

    private static RegistrationFlowException failure(Step step, String field, String code, String message,
            long retryAfter, PreparedRegistrationResponse prepared) {
        return new RegistrationFlowException(step, field, code, message, retryAfter,
                new RegistrationDetails(prepared.fullName(), prepared.username(), prepared.email()));
    }

    private static boolean isLoginIdentifierConflict(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            String name = cause instanceof org.hibernate.exception.ConstraintViolationException constraint
                    ? constraint.getConstraintName()
                    : cause.getMessage();
            if (name != null) {
                String normalized = name.toLowerCase(Locale.ROOT);
                if (normalized.contains("uq_user_username") || normalized.contains("uq_user_email"))
                    return true;
            }
        }
        return false;
    }

    private static String emailKey(String email) {
        return email.trim().toLowerCase();
    }

    private static long secondsUntil(Instant deadline, Instant now) {
        long millis = Duration.between(now, deadline).toMillis();
        return millis <= 0 ? 0 : (millis + 999) / 1000;
    }

    private static final class PendingRegistration {

        private final PreparedRegistrationResponse registration;
        private final String otpHash;
        private final Instant expiresAt;
        private int failedAttempts;

        private PendingRegistration(PreparedRegistrationResponse registration, String otpHash, Instant expiresAt) {
            this.registration = registration;
            this.otpHash = otpHash;
            this.expiresAt = expiresAt;
        }

        @Override
        public String toString() {
            return "PendingRegistration[redacted]";
        }
    }
}
