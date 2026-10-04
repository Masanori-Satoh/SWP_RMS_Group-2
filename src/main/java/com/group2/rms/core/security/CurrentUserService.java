package com.group2.rms.core.security;

import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository users;

    @Transactional(readOnly = true)
    public User requireUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Please log in.");
        }
        return users.findByUsernameIgnoreCase(authentication.getName())
                .filter(user -> "Active".equals(user.getAccountStatus()))
                .orElseThrow(() -> new AccessDeniedException("An active account is required."));
    }

    public static boolean hasRole(User user, String role) {
        return user.getRole() != null && role.equals(user.getRole().getRoleName());
    }

}
