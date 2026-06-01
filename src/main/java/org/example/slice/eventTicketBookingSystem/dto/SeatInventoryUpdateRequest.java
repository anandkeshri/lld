package org.example.slice.eventTicketBookingSystem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeatInventoryUpdateRequest {

    @Min(value = 1, message = "Original capacity must be at least 1")
    private int originalCapacity;

    @NotNull(message = "Pricing is required")
    @DecimalMin(value = "0.00", message = "Pricing must be non-negative")
    private BigDecimal pricing;
}
