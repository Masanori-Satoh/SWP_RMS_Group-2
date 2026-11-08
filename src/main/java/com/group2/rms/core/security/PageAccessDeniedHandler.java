package com.group2.rms.core.security;

import com.group2.rms.dashboard.shared.DashboardAccess;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import java.io.IOException;

/** Shows a one-time toast when an authenticated user opens a forbidden HTML page. */
public final class PageAccessDeniedHandler implements AccessDeniedHandler {
    private static final AccessDeniedHandler DEFAULT_HANDLER = new AccessDeniedHandlerImpl();
    private static final AuthenticationTrustResolverImpl TRUST = new AuthenticationTrustResolverImpl();
    private static final SessionFlashMapManager FLASH_MAPS = new SessionFlashMapManager();
    private static final RequestMatcher NON_PAGE_GET = new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher("/api/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/admin/api-monitoring/internal/health"),
            PathPatternRequestMatcher.withDefaults().matcher("/talent-pool/*/notes"),
            PathPatternRequestMatcher.withDefaults().matcher("/talent-pool/applications/*/bookmark"),
            PathPatternRequestMatcher.withDefaults().matcher("/applications/*/cv"),
            PathPatternRequestMatcher.withDefaults().matcher("/portal/applications/*/cv"),
            PathPatternRequestMatcher.withDefaults().matcher("/talent-pool/*/cv/*"));

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException, ServletException {
        if (!redirectIfPage(request, response, exception)) {
            DEFAULT_HANDLER.handle(request, response, exception);
        }
    }

    public static boolean redirectIfPage(HttpServletRequest request, HttpServletResponse response,
                                         AccessDeniedException exception) {
        Authentication actor = SecurityContextHolder.getContext().getAuthentication();
        if (exception instanceof CsrfException || response.isCommitted() || !TRUST.isAuthenticated(actor)
                || !isPageNavigation(request)) {
            return false;
        }

        String home = hasAuthority(actor, "ROLE_CANDIDATE") ? DashboardAccess.CANDIDATE_HOME_URL
                : hasAuthority(actor, RoleAuthorities.SYSTEM_ADMIN) ? DashboardAccess.ADMIN_DASHBOARD_URL
                : DashboardAccess.INTERNAL_DASHBOARD_URL;
        String target = request.getContextPath() + home;
        if (target.equals(request.getRequestURI())) {
            return false; // The destination itself was denied; do not redirect in a loop.
        }

        // A filter runs before DispatcherServlet creates the MVC output FlashMap.
        FlashMap flash = new FlashMap();
        flash.setTargetRequestPath(target);
        flash.put("accessDeniedMessage", "Bạn không có quyền truy cập vào trang này.");
        FLASH_MAPS.saveOutputFlashMap(flash, request, response);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.LOCATION, target);
        response.setStatus(HttpStatus.SEE_OTHER.value());
        return true;
    }

    private static boolean isPageNavigation(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod()) || NON_PAGE_GET.matches(request)
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            return false;
        }
        String mode = request.getHeader("Sec-Fetch-Mode");
        String destination = request.getHeader("Sec-Fetch-Dest");
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return (mode == null || "navigate".equals(mode))
                && (destination == null || "document".equals(destination))
                && accept != null && accept.contains(MediaType.TEXT_HTML_VALUE);
    }

    private static boolean hasAuthority(Authentication actor, String authority) {
        return actor.getAuthorities().stream().anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
