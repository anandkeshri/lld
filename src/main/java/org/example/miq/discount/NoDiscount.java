package org.example.miq.discount;

public class NoDiscount implements DiscountCalculator{
    int discount = 0;
    @Override
    public double caluclate(double amount) {
        return (amount*discount)/100;
    }
}
