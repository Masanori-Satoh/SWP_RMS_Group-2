package com.group2.rms.core.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;


@Constraint(validatedBy = PasswordMatchesValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatches {
    // default message error when
    String message() default "Password do not match";

    // groups attribute to validate different groups
    Class<?>[] groups() default {};

    // payload attribute
    Class<? extends Payload>[] payload() default {};

    String passwordField() default "password";

    String confirmPasswordField() default "confirmPassword";
}
