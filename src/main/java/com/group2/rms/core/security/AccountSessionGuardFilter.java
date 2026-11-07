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

import jakarta.servlet.http.HttpSession;

import java.io.IOException;

public class AccountSessionGuardFilter extends OncePerRequestFilter {

    /** Session attribute holding the password hash this session was authenticated with. */
    public static final String PASSWORD_HASH = AccountSessionGuardFilter.class.getName() + ".PASSWORD_HASH";

    private final UserRepository userRepository;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public AccountSessionGuardFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @SuppressWarnings("null")
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
            boolean passwordUnchanged = account == null || passwordUnchanged(request, account);
            if (account == null || !"Active".equals(account.getAccountStatus()) || !roleUnchanged
                    || !passwordUnchanged) {
                // Background calls (notification polling) must not end the session silently:
                // the next page click then gets the redirect that shows the "session expired" notice.
                if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                logoutHandler.logout(request, response, authentication);
                response.sendRedirect(request.getContextPath() + "/login?session-expired");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /** First request of a session remembers the password hash; if it changes later, the session ends. */
    private static boolean passwordUnchanged(HttpServletRequest request, User account) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return true;
        }
        Object remembered = session.getAttribute(PASSWORD_HASH);
        if (remembered == null) {
            session.setAttribute(PASSWORD_HASH, account.getPasswordHash());
            return true;
        }
        return remembered.equals(account.getPasswordHash());
    }
}
