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
        //get authentication
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        //check if authentication is not  exist and not authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Please log in.");
        }
        //get user by username from authentication
        return users.findByUsernameIgnoreCase(authentication.getName())
                //check if user is active
                .filter(user -> "Active".equals(user.getAccountStatus()))
                .orElseThrow(() -> new AccessDeniedException("An active account is required."));
    }

    public static boolean hasRole(User user, String role) {
        //check if  role is match
        return user.getRole() != null && role.equals(user.getRole().getRoleName());
    }

}
