package com.group2.rms.core.security;

import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AccountSessionGuardFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public AccountSessionGuardFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // check authentication is valid
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            User account = userRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
            String currentAuthority = null;
            // check if account and role is valid
            if (account != null && account.getRole() != null) {
                try {
                    currentAuthority = RoleAuthorities.fromRoleName(account.getRole().getRoleName());
                } catch (IllegalArgumentException ignored) {
                    // Unknown roles are denied below.
                }
            }

            String expectedAuthority = currentAuthority;
            boolean roleUnchanged = expectedAuthority != null
                    && authentication.getAuthorities().stream()
                    .anyMatch(granted -> expectedAuthority.equals(granted.getAuthority()));
            // check if account is active and role is unchanged
            if (account == null || !"Active".equals(account.getAccountStatus()) || !roleUnchanged) {
                logoutHandler.logout(request, response, authentication);
                response.sendRedirect(request.getContextPath() + "/login?session-expired");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
