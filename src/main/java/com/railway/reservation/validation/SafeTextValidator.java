package com.railway.reservation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SafeTextValidator implements ConstraintValidator<SafeText, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return SafeTextRules.isSafe(value);
    }
}
