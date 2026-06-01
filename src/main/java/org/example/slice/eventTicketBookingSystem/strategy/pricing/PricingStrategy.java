package org.example.slice.eventTicketBookingSystem.strategy.pricing;

import org.example.slice.eventTicketBookingSystem.model.SeatInventory;

import java.math.BigDecimal;

public interface PricingStrategy {
    BigDecimal calculateUnitPrice(SeatInventory inventory);
    String strategyName();
}
