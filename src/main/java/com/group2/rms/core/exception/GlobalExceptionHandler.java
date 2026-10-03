package com.group2.rms.core.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler providing centralized error handling across all controllers.
 * Preserves standard Spring MVC status codes (404, 403, 400) and prevents root causes from being masked.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found at [{}]: {}", request.getRequestURL(), ex.getMessage());
        ModelAndView mav = new ModelAndView("error/404");
        mav.setStatus(HttpStatus.NOT_FOUND);
        mav.addObject("message", ex.getMessage());
        mav.addObject("url", request.getRequestURL());
        return mav;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Static resource not found: [{}]", request.getRequestURL());
        ModelAndView mav = new ModelAndView("error/404");
        mav.setStatus(HttpStatus.NOT_FOUND);
        mav.addObject("message", "The requested resource could not be found.");
        mav.addObject("url", request.getRequestURL());
        return mav;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        log.warn("ResponseStatusException [{}] at [{}]: {}", status, request.getRequestURL(), ex.getReason());

        String viewName = status == HttpStatus.NOT_FOUND ? "error/404"
                        : status == HttpStatus.FORBIDDEN ? "error/403"
                        : "error/500";

        ModelAndView mav = new ModelAndView(viewName);
        mav.setStatus(status);
        mav.addObject("message", ex.getReason() != null ? ex.getReason() : ex.getMessage());
        mav.addObject("url", request.getRequestURL());
        return mav;
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for [{}] at [{}]: {}",
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Anonymous",
                request.getRequestURL(), ex.getMessage());
        ModelAndView mav = new ModelAndView("error/403");
        mav.setStatus(HttpStatus.FORBIDDEN);
        mav.addObject("message", "Bạn không có quyền truy cập vào chức năng hoặc tài nguyên này.");
        mav.addObject("url", request.getRequestURL());
        return mav;
    }

    @ExceptionHandler(BaseBusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleBusinessException(BaseBusinessException ex, HttpServletRequest request) {
        log.warn("Business exception [{}] at [{}]: {}", ex.getErrorCode(), request.getRequestURL(), ex.getMessage());
        ModelAndView mav = new ModelAndView("error/500");
        mav.setStatus(HttpStatus.BAD_REQUEST);
        mav.addObject("message", ex.getMessage());
        mav.addObject("errorCode", ex.getErrorCode());
        mav.addObject("url", request.getRequestURL());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleGlobalException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled system exception at [{}]: ", request.getRequestURL(), ex);
        ModelAndView mav = new ModelAndView("error/500");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("message", "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau.");
        mav.addObject("url", request.getRequestURL());
        return mav;
    }
}
