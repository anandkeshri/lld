package org.example.slice.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.example.slice.validation.annotation.NoSpecialCharacters;

public class NoSpecialCharactersValidator implements ConstraintValidator<NoSpecialCharacters, String> {

    // allows letters (any script), spaces, and hyphens
    private static final String PATTERN = "^[\\p{L} -]+$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // defer null/blank check to @NotBlank
        }
        return value.matches(PATTERN);
    }
}