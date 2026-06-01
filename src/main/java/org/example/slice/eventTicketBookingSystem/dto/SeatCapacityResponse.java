package org.example.slice.eventTicketBookingSystem.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SeatCapacityResponse {

    private Long eventId;
    private String seatType;
    private int originalCapacity;
    private int availableCapacity;
    private int soldCount;           // originalCapacity - availableCapacity
    private BigDecimal basePrice;
    private BigDecimal currentPrice; // computed by DynamicPricingStrategy — informational
}
