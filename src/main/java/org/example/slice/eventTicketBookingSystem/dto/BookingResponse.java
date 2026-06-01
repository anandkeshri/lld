package org.example.slice.eventTicketBookingSystem.dto;

import lombok.Builder;
import lombok.Data;
import org.example.slice.eventTicketBookingSystem.model.OrderStatus;
import org.example.slice.eventTicketBookingSystem.model.OrderType;
import org.example.slice.eventTicketBookingSystem.model.TicketStatus;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BookingResponse {

    private Long orderId;
    private String userId;
    private Long eventId;
    private String seatType;
    private OrderType orderType;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String bookingStrategy;
    private String pricingStrategy;
    private List<TicketInfo> tickets;

    @Data
    @Builder
    public static class TicketInfo {
        private Long ticketId;
        private String ticketNumber;
        private BigDecimal price;
        private TicketStatus status;
    }
}
