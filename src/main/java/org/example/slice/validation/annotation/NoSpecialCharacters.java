package org.example.slice.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.example.slice.validation.validator.NoSpecialCharactersValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NoSpecialCharactersValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoSpecialCharacters {

    String message() default "Must contain only letters, spaces, or hyphens";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}