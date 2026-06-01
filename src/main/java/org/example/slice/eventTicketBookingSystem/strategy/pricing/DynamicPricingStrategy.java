package org.example.slice.eventTicketBookingSystem.strategy.pricing;

import org.example.slice.eventTicketBookingSystem.model.SeatInventory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class DynamicPricingStrategy implements PricingStrategy {

    private static final BigDecimal MULTIPLIER_BASE    = new BigDecimal("1.00");
    private static final BigDecimal MULTIPLIER_TIER_1  = new BigDecimal("1.25");
    private static final BigDecimal MULTIPLIER_TIER_2  = new BigDecimal("1.50");
    private static final BigDecimal MULTIPLIER_SURGE   = new BigDecimal("2.00");

    @Override
    public BigDecimal calculateUnitPrice(SeatInventory inventory) {
        int original = inventory.getOriginalCapacity();
        if (original == 0) {
            return inventory.getPricing();
        }
        int sold = original - inventory.getAvailableCapacity().get();
        double fillRate = (double) sold / original;

        BigDecimal multiplier = resolveMultiplier(fillRate);
        return inventory.getPricing().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String strategyName() {
        return "DYNAMIC";
    }

    private BigDecimal resolveMultiplier(double fillRate) {
        if (fillRate < 0.50) return MULTIPLIER_BASE;
        if (fillRate < 0.75) return MULTIPLIER_TIER_1;
        if (fillRate < 0.90) return MULTIPLIER_TIER_2;
        return MULTIPLIER_SURGE;
    }
}
