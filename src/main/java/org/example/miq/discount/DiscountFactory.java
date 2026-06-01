package org.example.miq.discount;

public class DiscountFactory {
    public static DiscountCalculator getCalculator(String card){
        if(card.charAt(0) == '4'){
            return new VisaDiscountCalculator();
        }
        else if(card.charAt(0) == '5'){
            return new MasterCardDiscountCalculator();
        }
        else if(card.charAt(0) == '3' && card.charAt(1) == '7'){
            return new AmexDiscountCalculator();
        }

        return new NoDiscount();
    }
}
