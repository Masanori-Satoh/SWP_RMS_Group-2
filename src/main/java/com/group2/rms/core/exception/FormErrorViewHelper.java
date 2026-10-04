package com.group2.rms.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * FormErrorViewHelper
 */
public class FormErrorViewHelper {

    public ModelAndView renderValidationError(BindingResult bindingResult, HttpServletRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'renderValidationError'");
    }

    public ModelAndView renderFieldError(String field, String message, HttpServletRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'renderFieldError'");
    }



	public ModelAndView renderConflictError(String message, HttpServletRequest request) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'renderConflictError'");
	}

    public ModelAndView renderForgotPasswordError(String message, HttpServletRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'renderForgotPasswordError'");
    }

    public ModelAndView renderForgotPasswordError(String string, String message, HttpServletRequest request,
            HttpStatus serviceUnavailable) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'renderForgotPasswordError'");
    }

    public ModelAndView renderInvalidTokenError(String message, HttpServletRequest request,
            HttpServletResponse response) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'renderInvalidTokenError'");
    }

    
}
