package org.example.slice.eventTicketBookingSystem.validation;

import org.example.slice.eventTicketBookingSystem.model.Event;
import org.example.slice.exception.BadRequestException;
import org.example.slice.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class EventValidator {

    public void validateForCreate(Event event) {
        validateTimeConstraints(event);
        validateScheduledTimeInFuture(event.getScheduledTime());
        validateSeatTypes(event.getSeatTypes());
    }

    public void validateForUpdate(Event event) {
        validateTimeConstraints(event);
        validateSeatTypes(event.getSeatTypes());
    }

    private void validateTimeConstraints(Event event) {
        if (!event.getSaleStartTime().isBefore(event.getSaleEndTime())) {
            throw new BusinessRuleViolationException(
                    "INVALID_SALE_WINDOW",
                    "Sale start time must be before sale end time"
            );
        }
        if (!event.getSaleEndTime().isBefore(event.getScheduledTime())) {
            throw new BusinessRuleViolationException(
                    "INVALID_SALE_END_TIME",
                    "Sale end time must be before the event's scheduled time"
            );
        }
    }

    private void validateScheduledTimeInFuture(LocalDateTime scheduledTime) {
        if (!scheduledTime.isAfter(LocalDateTime.now())) {
            throw new BusinessRuleViolationException(
                    "PAST_SCHEDULED_TIME",
                    "Scheduled time must be in the future"
            );
        }
    }

    private void validateSeatTypes(List<String> seatTypes) {
        if (seatTypes == null || seatTypes.isEmpty()) {
            throw new BadRequestException("At least one seat type is required");
        }
        for (String seatType : seatTypes) {
            if (seatType == null || seatType.isBlank()) {
                throw new BadRequestException("Seat type values must not be blank");
            }
        }
        Set<String> unique = new HashSet<>(seatTypes);
        if (unique.size() != seatTypes.size()) {
            throw new BadRequestException("Duplicate seat types are not allowed in the same event");
        }
    }
}
