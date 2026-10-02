package com.group2.rms.auth;

import com.group2.rms.user.entity.Role;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.service.AccountManagementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public registration creates only Candidate accounts and their separate profile. */
@Service
public class CandidateRegistrationService {

    private final RoleRepository roles;
    private final AccountManagementService accounts;

    public CandidateRegistrationService(RoleRepository roles, AccountManagementService accounts) {
        this.roles = roles;
        this.accounts = accounts;
    }

    @Transactional
    public int register(RegisterCommand command) {
        Role candidate = roles.findByRoleName("Candidate")
                .orElseThrow(() -> new IllegalStateException("Candidate role is not configured"));
        return accounts.create(new AccountManagementService.CreateCommand(
                command.fullName(), command.username(), command.email(), null,
                candidate.getRoleId(), null, command.password()));
    }

    public record RegisterCommand(String fullName, String username, String email, String password) {
    }
}
