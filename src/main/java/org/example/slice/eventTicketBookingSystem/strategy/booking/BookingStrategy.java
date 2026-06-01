package org.example.slice.eventTicketBookingSystem.strategy.booking;

public interface BookingStrategy {
    BookingResult execute(BookingContext context);
    String strategyName();
}
