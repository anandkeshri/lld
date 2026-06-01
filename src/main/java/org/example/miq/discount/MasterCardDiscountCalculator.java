package org.example.miq.discount;

public class MasterCardDiscountCalculator implements DiscountCalculator {
    int discount = 15;

    @Override
    public double caluclate(double amount) {
        return (amount * discount) / 100;
    }
}