package org.example.miq.discount;

public class VisaDiscountCalculator implements DiscountCalculator{
    int discount = 10;
    @Override
    public double caluclate(double amount) {
        return (amount*discount)/100;
    }
}
