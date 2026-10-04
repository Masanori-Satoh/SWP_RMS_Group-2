package com.group2.rms.interview.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom Class-level Constraint Annotation để kiểm tra ràng buộc thời gian phỏng vấn:
 * Rule MSG26: endTime phải lớn hơn startTime.
 */
@Documented
@Constraint(validatedBy = ValidInterviewTimeValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidInterviewTime {

    String message() default "Thời gian kết thúc (endTime) phải lớn hơn thời gian bắt đầu (startTime).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
