package org.example.slice.eventTicketBookingSystem.dto;

import lombok.Builder;
import lombok.Data;
import org.example.slice.eventTicketBookingSystem.model.OrderStatus;
import org.example.slice.eventTicketBookingSystem.model.OrderType;
import org.example.slice.eventTicketBookingSystem.model.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BookingHistoryResponse {

    private Long orderId;
    private String userId;
    private Long eventId;
    private String seatType;
    private OrderType orderType;
    private int quantity;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private Long referenceOrderId;  // non-null for CANCEL orders — points to original BOOK order
    private LocalDateTime createdAt;
    private List<TicketInfo> tickets;  // populated only for BOOK orders

    @Data
    @Builder
    public static class TicketInfo {
        private Long ticketId;
        private String ticketNumber;
        private BigDecimal price;
        private TicketStatus status;
    }
}
