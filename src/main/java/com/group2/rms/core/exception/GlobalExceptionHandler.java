package com.group2.rms.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", ex.getMessage());
        mav.addObject("url", request.getRequestURL());
        return mav;
    }

    @ExceptionHandler(BaseBusinessException.class)
    public ModelAndView handleBusinessException(BaseBusinessException ex, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", ex.getMessage());
        mav.addObject("errorCode", ex.getErrorCode());
        mav.addObject("url", request.getRequestURL());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleGlobalException(Exception ex, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau.");
        mav.addObject("url", request.getRequestURL());
        return mav;
    }
}
