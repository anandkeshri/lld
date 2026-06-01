package org.example.slice.eventTicketBookingSystem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotBlank(message = "Seat type is required")
    private String seatType;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10, message = "Quantity cannot exceed 10 per order")
    private int quantity;

    private String bookingStrategy;  // PESSIMISTIC | OPTIMISTIC — defaults to system config
    private String pricingStrategy;  // FIXED | DYNAMIC — defaults to system config
}
