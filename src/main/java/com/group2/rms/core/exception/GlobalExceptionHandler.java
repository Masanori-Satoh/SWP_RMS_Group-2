package com.group2.rms.core.exception;

import com.group2.rms.dto.response.ApiResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        String contentType = request.getHeader("Content-Type");
        return (uri != null && uri.startsWith("/api/"))
                || (accept != null && accept.contains("application/json"))
                || (contentType != null && contentType.contains("application/json"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDto<>(false, ex.getMessage()));
        }
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", ex.getMessage());
        mav.addObject("url", request.getRequestURL());
        return mav;
    }

    @ExceptionHandler(BaseBusinessException.class)
    public Object handleBusinessException(BaseBusinessException ex, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto<>(false, ex.getMessage()));
        }
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", ex.getMessage());
        mav.addObject("errorCode", ex.getErrorCode());
        mav.addObject("url", request.getRequestURL());
        return mav;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("Dữ liệu gửi lên không hợp lệ.");
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto<>(false, message));
        }
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", message);
        mav.addObject("url", request.getRequestURL());
        return mav;
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public Object handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = "Ứng viên này đã có một đề xuất Offer trong hệ thống. Không thể tạo trùng lặp.";
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponseDto<>(false, message));
        }
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", message);
        mav.addObject("url", request.getRequestURL());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public Object handleGlobalException(Exception ex, HttpServletRequest request) {
        if (isApiRequest(request)) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau.";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDto<>(false, msg));
        }
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau.");
        mav.addObject("url", request.getRequestURL());
        return mav;
    }
}
