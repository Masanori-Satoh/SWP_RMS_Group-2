package com.group2.rms.core.validation;

import org.springframework.beans.BeanWrapperImpl;

import jakarta.validation.ConstraintDeclarationException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    private String passwordField;
    private String confirmPasswordField;

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // check if object dto is null
        if (value == null) {
            return true;
        }
        BeanWrapperImpl wrapper = new BeanWrapperImpl(value);
        Object password = wrapper.getPropertyValue(passwordField);
        Object confirmPassword = wrapper.getPropertyValue(confirmPasswordField);

        String passwordStr = password != null ? password.toString() : "";
        String confirmPasswordStr = confirmPassword != null ? confirmPassword.toString() : "";

        // check a field is blank
        if (passwordStr.isBlank() || confirmPasswordStr.isBlank()) {
            return true;
        }
        boolean matched = passwordStr.equals(confirmPasswordStr);

        if (!matched) {
            // turn off default message
            context.disableDefaultConstraintViolation();
            // add custom message
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    // set the field has error
                    .addPropertyNode(confirmPasswordField)
                    // add violet
                    .addConstraintViolation();
            return false;
        }
        return matched;
    }

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
        this.passwordField = constraintAnnotation.passwordField();
        this.confirmPasswordField = constraintAnnotation.confirmPasswordField();
        // check if config is wrong password, confirm is null or equal
        if (passwordField.isBlank() || confirmPasswordField.isBlank() || passwordField.equals(confirmPasswordField)) {
            throw new ConstraintDeclarationException(
                    "@PasswordMatches requires two distinct property names.");

        }
    }

}
