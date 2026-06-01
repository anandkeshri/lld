package org.example.slice.eventTicketBookingSystem.strategy.booking;

import lombok.Builder;
import lombok.Getter;
import org.example.slice.eventTicketBookingSystem.model.Order;
import org.example.slice.eventTicketBookingSystem.model.SeatInventory;

import java.math.BigDecimal;

@Getter
@Builder
public class BookingContext {
    private String userId;
    private Long eventId;
    private String seatType;
    private int quantity;
    private Order order;
    private SeatInventory inventory;
    private BigDecimal unitPrice;
}
