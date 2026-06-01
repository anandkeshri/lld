package org.example.slice.validation;

import org.example.slice.exception.BusinessRuleViolationException;
import org.example.slice.exception.InvalidOperationException;
import org.example.slice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserValidatorTest {

    private UserValidator userValidator;

    @BeforeEach
    void setUp() {
        userValidator = new UserValidator();
    }

    @Test
    void validateForCreate_validEmail_doesNotThrow() {
        User user = new User(null, "Jane", "jane@gmail.com");

        assertThatNoException().isThrownBy(() -> userValidator.validateForCreate(user));
    }

    @Test
    void validateForCreate_blockedEmailDomain_throwsBusinessRuleViolationException() {
        User user = new User(null, "Spammer", "spammer@mailinator.com");

        assertThatThrownBy(() -> userValidator.validateForCreate(user))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void validateForUpdate_withChanges_doesNotThrow() {
        User existing = new User(1L, "Jane", "jane@gmail.com");
        User incoming = new User(1L, "Jane Updated", "jane@gmail.com");

        assertThatNoException().isThrownBy(() -> userValidator.validateForUpdate(existing, incoming));
    }

    @Test
    void validateForUpdate_noChanges_throwsInvalidOperationException() {
        User existing = new User(1L, "Jane", "jane@gmail.com");
        User incoming = new User(1L, "Jane", "jane@gmail.com");

        assertThatThrownBy(() -> userValidator.validateForUpdate(existing, incoming))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void validateForUpdate_blockedEmailDomain_throwsBusinessRuleViolationException() {
        User existing = new User(1L, "Jane", "jane@gmail.com");
        User incoming = new User(1L, "Jane", "jane@yopmail.com");

        assertThatThrownBy(() -> userValidator.validateForUpdate(existing, incoming))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}