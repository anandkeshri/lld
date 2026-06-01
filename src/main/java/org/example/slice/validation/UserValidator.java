package org.example.slice.validation;

import org.example.slice.exception.BusinessRuleViolationException;
import org.example.slice.exception.InvalidOperationException;
import org.example.slice.model.User;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UserValidator {

    private static final Set<String> BLOCKED_EMAIL_DOMAINS = Set.of(
            "mailinator.com", "tempmail.com", "throwaway.email", "guerrillamail.com", "yopmail.com"
    );

    public void validateForCreate(User user) {
        validateEmailDomain(user.getEmail());
    }

    public void validateForUpdate(User existing, User incoming) {
        validateEmailDomain(incoming.getEmail());
        validateHasChanges(existing, incoming);
    }

    private void validateEmailDomain(String email) {
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        if (BLOCKED_EMAIL_DOMAINS.contains(domain)) {
            throw new BusinessRuleViolationException(
                    "BLOCKED_EMAIL_DOMAIN",
                    "Registrations from '" + domain + "' are not allowed"
            );
        }
    }

    // Prevents no-op updates from reaching the DB
    private void validateHasChanges(User existing, User incoming) {
        boolean nameUnchanged = existing.getName().equals(incoming.getName());
        boolean emailUnchanged = existing.getEmail().equals(incoming.getEmail());
        if (nameUnchanged && emailUnchanged) {
            throw new InvalidOperationException("No changes detected — update request is identical to current state");
        }
    }
}