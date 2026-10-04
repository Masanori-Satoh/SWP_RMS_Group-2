package com.group2.rms.auth.service;

import com.group2.rms.auth.exception.InvalidResetTokenException;
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
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
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
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Pattern TOKEN_FORMAT = Pattern.compile(
            "v1\\.([1-9][0-9]{0,9})\\.([0-9]{1,15})\\.([A-Za-z0-9_-]{32})\\.([A-Za-z0-9_-]{43})");
    private static final long EXPIRY_SECONDS = 15 * 60;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final byte[] signingKey;

    @Autowired
    public PasswordResetService(UserRepository users, PasswordEncoder passwordEncoder,
            @Value("${app.password.reset.secret:}") String secret) {
        this(users, passwordEncoder, secret, Clock.systemUTC());
    }

    public PasswordResetService(UserRepository users, PasswordEncoder passwordEncoder,
            String secret, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        byte[] suppliedKey = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = suppliedKey.length >= 32 ? suppliedKey : null;
    }

    public boolean isConfigured() {
        return signingKey != null;
    }

    @Transactional(readOnly = true)
    public Optional<ResetLink> request(String email) {
        if (!isConfigured() || email == null || email.isBlank()) {
            return Optional.empty();
        }
        User user = users.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (user == null || !"Active".equals(user.getAccountStatus())) {
            return Optional.empty();
        }
        byte[] nonce = new byte[24];
        RANDOM.nextBytes(nonce);
        long expiresAt = clock.instant().getEpochSecond() + EXPIRY_SECONDS;
        String payload = "v1." + user.getUserId() + "." + expiresAt + "." + ENCODER.encodeToString(nonce);
        String signature = ENCODER.encodeToString(sign(payload, user));
        String otp = generateOtp(payload, user);
        return Optional.of(new ResetLink(user.getEmail(), payload + "." + signature, otp));
    }

    @Transactional(readOnly = true)
    public boolean isValid(String token) {
        ParsedToken parsed = parse(token);
        if (parsed == null) {
            return false;
        }
        return users.findById(parsed.userId())
                .filter(user -> validForUser(parsed, user))
                .isPresent();
    }

    @Transactional
    public void reset(String token, String otp, String newPassword) {
        ParsedToken parsed = parse(token);
        if (parsed == null) {
            throw new InvalidResetTokenException("This link is invalid, used, or expired.");
        }
        if (newPassword == null || newPassword.isBlank()
                || newPassword.length() < 8 || newPassword.length() > 32) {
            throw new AccountFieldException("password", "Password must contain 8–32 characters.");
        }
        User user = users.findByIdForUpdate(parsed.userId()).orElse(null);
        if (user == null || !validForUser(parsed, user)) {
            throw new InvalidResetTokenException();
        }
        // Check OTP code
        String expectedOtp = generateOtp(parsed.payload(), user);
        if (otp == null || !MessageDigest.isEqual(
                expectedOtp.getBytes(StandardCharsets.UTF_8),
                otp.trim().getBytes(StandardCharsets.UTF_8))) {
            throw new AccountFieldException("otp", "Invalid OTP code.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.saveAndFlush(user);

    }

    private String generateOtp(String payload, User user) {
        byte[] hash = sign("OTP:" + payload, user);
        int code = ((hash[0] & 0x7f) << 24)
                | ((hash[1] & 0xff) << 16)
                | ((hash[2] & 0xff) << 8)
                | (hash[3] & 0xff);
        return String.format("%06d", code % 1_000_000);
    }

    private ParsedToken parse(String token) {
        if (!isConfigured() || token == null) {
            return null;
        }
        Matcher matcher = TOKEN_FORMAT.matcher(token);
        if (!matcher.matches()) {
            return null;
        }
        try {
            int userId = Integer.parseInt(matcher.group(1));
            long expiry = Long.parseLong(matcher.group(2));
            byte[] signature = Base64.getUrlDecoder().decode(matcher.group(4));
            if (signature.length != 32) {
                return null;
            }
            String payload = token.substring(0, token.lastIndexOf('.'));
            return new ParsedToken(userId, expiry, payload, signature);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean validForUser(ParsedToken parsed, User user) {
        long now = clock.instant().getEpochSecond();
        if (!"Active".equals(user.getAccountStatus()) || parsed.expiresAt() <= now
                || parsed.expiresAt() > now + EXPIRY_SECONDS) {
            return false;
        }
        byte[] expected = sign(parsed.payload(), user);
        return MessageDigest.isEqual(expected, parsed.signature());
    }

    private byte[] sign(String payload, User user) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            // Email and the BCrypt hash are part of the MAC, never included in the link.
            String boundPayload = payload + "\n" + user.getEmail() + "\n" + user.getPasswordHash();
            return mac.doFinal(boundPayload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }

    private record ParsedToken(int userId, long expiresAt, String payload, byte[] signature) {
    }

    public record ResetLink(String email, String token, String otp) {
    }
}
