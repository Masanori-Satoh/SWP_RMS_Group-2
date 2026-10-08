package com.group2.rms.auth.service;

import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.service.AccountManagementService;
import com.group2.rms.user.exception.AccountFieldException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public registration creates only Candidate accounts and their separate profile. */
@Service
public class CandidateRegistrationService {

    private final RoleRepository roles;
    private final AccountManagementService accounts;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    public CandidateRegistrationService(RoleRepository roles, AccountManagementService accounts,
            PasswordEncoder passwordEncoder, Validator validator) {
        this.roles = roles;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public PreparedRegistration prepare(RegisterCommand command) {
        var violations = validator.validate(command);
        if (!violations.isEmpty()) {
            var violation = violations.iterator().next();
            throw new AccountFieldException(violation.getPropertyPath().toString(), violation.getMessage());
        }
        roles.findByRoleName("Candidate")
                .orElseThrow(() -> new IllegalStateException("Candidate role is not configured"));
        String username = command.username().trim();
        String email = command.email().trim();
        accounts.assertLoginIdentifiersAvailable(username, email);
        return new PreparedRegistration(command.fullName().trim(), username, email,
                passwordEncoder.encode(command.password()));
    }

    @Transactional
    public int registerVerified(PreparedRegistration registration) {
        return accounts.createCandidateWithPasswordHash(
                new AccountManagementService.CandidateWithPasswordHashCommand(
                        registration.fullName(), registration.username(), registration.email(),
                        registration.passwordHash()));
    }

    public record RegisterCommand(
            @NotBlank(message = "Enter a full name.")
            @Size(max = 100, message = "Full name must be at most 100 characters.") String fullName,
            @NotBlank(message = "Enter a username.")
            @Size(max = 50, message = "Username must be at most 50 characters.") String username,
            @NotBlank(message = "Enter an email address.")
            @Email(message = "Enter a valid email address.")
            @Size(max = 150, message = "Email must be at most 150 characters.") String email,
            @NotBlank(message = "Enter a password.")
            @Size(min = 8, max = 32, message = "Password must contain 8–32 characters.") String password) {
        @Override
        public String toString() {
            return "RegisterCommand[redacted]";
        }
    }

    public record PreparedRegistration(String fullName, String username, String email,
            String passwordHash) implements Serializable {
        @Override
        public String toString() {
            return "PreparedRegistration[redacted]";
        }
    }
}
