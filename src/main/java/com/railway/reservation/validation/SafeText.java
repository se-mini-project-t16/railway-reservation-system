package com.railway.reservation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Rejects HTML/script markup and SQL-injection payloads in free-text input (RRS-SR-003). */
@Documented
@Constraint(validatedBy = SafeTextValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeText {

    String message() default "contains characters or patterns that are not allowed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
