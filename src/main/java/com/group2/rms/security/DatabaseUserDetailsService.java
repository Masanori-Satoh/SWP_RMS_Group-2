package com.group2.rms.security;

import com.group2.rms.entity.User;
import com.group2.rms.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String login = identifier == null ? "" : identifier.trim();
        if (login.isEmpty()) {
            throw new UsernameNotFoundException("Invalid credentials");
        }

        Optional<User> byUsername = userRepository.findByUsernameIgnoreCase(login);
        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(login);
        if (byUsername.isPresent() && byEmail.isPresent()
                && !Objects.equals(byUsername.get().getUserId(), byEmail.get().getUserId())) {
            // A login identifier must never resolve to two different accounts.
            throw new UsernameNotFoundException("Invalid credentials");
        }

        User account = byUsername.or(() -> byEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        String authority;
        try {
            authority = RoleAuthorities.fromRoleName(account.getRole().getRoleName());
        } catch (RuntimeException ex) {
            throw new UsernameNotFoundException("Invalid credentials", ex);
        }

        return org.springframework.security.core.userdetails.User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .authorities(authority)
                .disabled(!"Active".equals(account.getAccountStatus()))
                .build();
    }
}
