package org.example.miq.discount;

public class AmexDiscountCalculator implements DiscountCalculator{
    int discount = 5;
    @Override
    public double caluclate(double amount) {
        return (amount*discount)/100;
    }
}
