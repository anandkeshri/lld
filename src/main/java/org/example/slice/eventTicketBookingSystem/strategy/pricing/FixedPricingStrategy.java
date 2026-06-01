package org.example.slice.eventTicketBookingSystem.strategy.pricing;

import org.example.slice.eventTicketBookingSystem.model.SeatInventory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FixedPricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal calculateUnitPrice(SeatInventory inventory) {
        return inventory.getPricing();
    }

    @Override
    public String strategyName() {
        return "FIXED";
    }
}
