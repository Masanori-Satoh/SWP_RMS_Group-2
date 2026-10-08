package com.group2.rms.core.exception;

import com.group2.rms.admin.dto.HealthResponse;
import com.group2.rms.admin.exception.DatabaseHealthException;
import com.group2.rms.auth.dto.ForgotPasswordRequest;
import com.group2.rms.auth.dto.ResetPasswordRequest;
import com.group2.rms.auth.dto.RegisterAccountRequest;
import com.group2.rms.auth.dto.VerifyRegistrationOtpRequest;
import com.group2.rms.auth.exception.RegistrationFlowException;
import com.group2.rms.auth.dto.VerifyPasswordResetOtpRequest;
import com.group2.rms.auth.exception.PasswordRecoveryFlowException;
import com.group2.rms.auth.exception.PasswordRecoveryUnavailableException;
import com.group2.rms.auth.exception.PasswordResetDeliveryException;
import com.group2.rms.user.exception.DepartmentFieldException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Centralized Global Exception Handler for RMS Project.
 * Preserves standard Spring MVC status codes (404, 403, 400, 503, 500)
 * and prevents sensitive system tracebacks from leaking to end users.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RegistrationFlowException.class)
    public ModelAndView handleRegistrationFlowException(RegistrationFlowException exception,
            HttpServletRequest request, RedirectAttributes attributes) {
        Object form;
        String target;
        if (exception.getStep() == RegistrationFlowException.Step.OTP) {
            form = new VerifyRegistrationOtpRequest();
            target = "/register/otp";
        } else {
            RegisterAccountRequest registrationForm = new RegisterAccountRequest();
            var details = exception.getDetails();
            registrationForm.setFullName(details == null ? request.getParameter("fullName") : details.fullName());
            registrationForm.setUsername(details == null ? request.getParameter("username") : details.username());
            registrationForm.setEmail(details == null ? request.getParameter("email") : details.email());
            form = registrationForm;
            target = "/register";
        }
        BindingResult errors = new BeanPropertyBindingResult(form, "form");
        if (exception.getField() == null || exception.getField().isBlank()) {
            errors.reject(exception.getErrorCode(), exception.getMessage());
        } else {
            errors.rejectValue(exception.getField(), exception.getErrorCode(), exception.getMessage());
        }
        attributes.addFlashAttribute("form", form);
        attributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "form", errors);
        if (exception.getRetryAfterSeconds() > 0) {
            attributes.addFlashAttribute("retryAfterSeconds", exception.getRetryAfterSeconds());
        }
        log.warn("Registration rejected [{}]", exception.getErrorCode());
        ModelAndView view = new ModelAndView("redirect:" + target);
        view.setStatus(HttpStatus.SEE_OTHER);
        return view;
    }

    @ExceptionHandler(DepartmentFieldException.class)
    public ModelAndView handleDepartmentFieldException(DepartmentFieldException exception,
            HttpServletRequest request, HttpServletResponse response) {
        String target = request.getContextPath() + "/admin/departments/"
                + (exception.getDepartmentId() == null ? "new" : exception.getDepartmentId() + "/edit");
        BindingResult errors = new BeanPropertyBindingResult(exception.getForm(), "form");
        errors.rejectValue(exception.getField(), "department.invalid", exception.getMessage());
        var flash = RequestContextUtils.getOutputFlashMap(request);
        flash.put("form", exception.getForm());
        flash.put(BindingResult.MODEL_KEY_PREFIX + "form", errors);
        RequestContextUtils.saveOutputFlashMap(target, request, response);
        ModelAndView view = new ModelAndView("redirect:" + target);
        view.setStatus(HttpStatus.SEE_OTHER);
        return view;
    }

    // 404 not found
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
        log.warn("Static resource not found at [{}]: {}", request.getRequestURL(), ex.getMessage());
        ModelAndView mav = new ModelAndView("error/404");
        mav.setStatus(HttpStatus.NOT_FOUND);
        mav.addObject("message", "Tài nguyên được yêu cầu không tồn tại.");
        mav.addObject("url", request.getRequestURL());
        return mav;
    }

    // 403 forbidden
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

    // 503 service unavailable
    @ExceptionHandler(DatabaseHealthException.class)
    public ResponseEntity<HealthResponse> handleDatabaseHealthException(DatabaseHealthException exception) {
        log.warn("Database health check failed [{}]", exception.getErrorCode());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new HealthResponse("DOWN"));
    }

    @ExceptionHandler({
            PasswordResetDeliveryException.class,
            PasswordRecoveryUnavailableException.class
    })
    public ModelAndView handlePasswordRecoveryMailFailure(BaseBusinessException exception, HttpServletRequest request,
            RedirectAttributes attributes) {

        ForgotPasswordRequest form = new ForgotPasswordRequest();
        form.setEmail(request.getParameter("email"));

        BindingResult errors = new BeanPropertyBindingResult(form, "form");

        errors.reject(
                exception.getErrorCode(),
                "Unable to send the verification email. Please try again later.");

        attributes.addFlashAttribute("form", form);

        attributes.addFlashAttribute(
                BindingResult.MODEL_KEY_PREFIX + "form",
                errors);

        log.error(
                "Password recovery mail failure [{}]",
                exception.getErrorCode());

        ModelAndView view = new ModelAndView("redirect:/forgot-password");

        view.setStatus(HttpStatus.SEE_OTHER);

        return view;
    }

    // 400 bad request
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

    // 400 validation error
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object handleMethodArgumentNotValidException(
            org.springframework.web.bind.MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String errorMsg = ex.getBindingResult().getAllErrors().stream()
                .map(org.springframework.context.support.DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse("Dữ liệu gửi lên không hợp lệ.");
        log.warn("Validation error at [{}]: {}", request.getRequestURL(), errorMsg);

        if (request.getRequestURI() != null && request.getRequestURI().contains("/api/")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new com.group2.rms.core.dto.ApiResponse<>(false, errorMsg));
        }

        ModelAndView mav = new ModelAndView("error/500");
        mav.setStatus(HttpStatus.BAD_REQUEST);
        mav.addObject("message", errorMsg);
        mav.addObject("url", request.getRequestURL());
        return mav;
    }

    @ExceptionHandler(PasswordRecoveryFlowException.class)
    public ModelAndView handlePasswordRecoveryFlowException(PasswordRecoveryFlowException exception,
            HttpServletRequest request, RedirectAttributes attributes) {

        String target = switch (exception.getStep()) {
            case FORGOT -> "/forgot-password";
            case OTP -> "/reset-password/otp";
            case PASSWORD -> "/reset-password";
        };

        // create empty form
        Object form = switch (exception.getStep()) {
            case FORGOT -> {
                ForgotPasswordRequest forgotForm = new ForgotPasswordRequest();
                // store email
                forgotForm.setEmail(request.getParameter("email"));
                yield forgotForm;
            }
            case OTP -> new VerifyPasswordResetOtpRequest();
            case PASSWORD -> new ResetPasswordRequest();
        };

        BindingResult errors = new BeanPropertyBindingResult(form, "form");
        // get field has error
        String field = exception.getField();
        // check if error is a field
        if (field != null && !field.isBlank()) {
            errors.rejectValue(field, exception.getErrorCode(), exception.getMessage());

        }
        // error is global
        else {
            errors.reject(exception.getErrorCode(), exception.getMessage());
        }

        attributes.addFlashAttribute("form", form);

        attributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "form", errors);
        // check if retry is remain
        if (exception.getRetryAfterSeconds() > 0) {
            attributes.addFlashAttribute(
                    "retryAfterSeconds", exception.getRetryAfterSeconds());
        }

        log.warn("Password recovery rejected [{}]", exception.getErrorCode());

        ModelAndView view = new ModelAndView("redirect:" + target);
        view.setStatus(HttpStatus.SEE_OTHER);

        return view;
    }

    // =========================================================================
    // 6. SPRING RESPONSE STATUS EXCEPTION HANDLER
    // =========================================================================

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

    // =========================================================================
    // 7. 500 INTERNAL SERVER ERROR (UNHANDLED SYSTEM FALLBACK)
    // =========================================================================

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
