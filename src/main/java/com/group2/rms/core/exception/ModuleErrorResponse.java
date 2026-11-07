package com.group2.rms.core.exception;

import com.group2.rms.core.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

/** Presentation only: no database, authorization or business decisions. */
public final class ModuleErrorResponse {
    private ModuleErrorResponse() { }

    public static Object reject(HttpServletRequest request, HttpServletResponse response, Object handler,
                                int status, String message, String destination) {
        if (wantsJson(request, handler)) {
            return ResponseEntity.status(status)
                    .header("Cache-Control", "no-store")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ApiResponse<>(false, message));
        }
        String target = request.getContextPath() + destination;
        RequestContextUtils.getOutputFlashMap(request).put("errorMessage", message);
        RequestContextUtils.saveOutputFlashMap(target, request, response);
        response.setHeader("Cache-Control", "no-store");
        ModelAndView redirect = new ModelAndView("redirect:" + destination);
        redirect.setStatus(HttpStatus.SEE_OTHER);
        return redirect;
    }

    private static boolean wantsJson(HttpServletRequest request, Object handler) {
        if (handler instanceof HandlerMethod method && method.hasMethodAnnotation(ResponseBody.class)) return true;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        // JSON-only routes can fail mapping before Spring selects a HandlerMethod.
        if (path.matches("/talent-pool/jobs/[^/]+/(suggest|recommend|selection-review)")
                || path.matches("/talent-pool/applications/[^/]+/bookmark(/remove)?")
                || ("GET".equals(request.getMethod()) && path.matches("/talent-pool/[^/]+/notes"))) return true;
        String accept = request.getHeader("Accept");
        String contentType = request.getContentType();
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE))
                || (contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON_VALUE));
    }
}
