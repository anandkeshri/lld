package org.example.slice.eventTicketBookingSystem.strategy.booking;

import lombok.Builder;
import lombok.Getter;
import org.example.slice.eventTicketBookingSystem.model.Order;
import org.example.slice.eventTicketBookingSystem.model.Ticket;

import java.util.List;

@Getter
@Builder
public class BookingResult {
    private Order order;
    private List<Ticket> tickets;
    private boolean success;
    private String failureReason;
}
