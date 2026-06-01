package org.example.miq;

import org.example.miq.discount.DiscountCalculator;
import org.example.miq.discount.DiscountFactory;
import org.example.miq.validator.CardValidator;
import org.example.miq.validator.DigitCountValidator;
import org.example.miq.validator.LunhValidator;

import java.util.ArrayList;

public class CardDiscount {

    ArrayList<CardValidator> validator = new ArrayList<>();
    DiscountCalculator calculator;

    public CardDiscount(){
        validator.add(new DigitCountValidator());
        validator.add(new LunhValidator());
    }

    public double calculateDiscount(double billAmount, String card){
        for(CardValidator val : validator){
            if(!val.validate(card)){
                System.out.println("Invalid card");
                return 0;
            }
        }
        calculator = DiscountFactory.getCalculator(card);

        return calculator.caluclate(billAmount);
    }
}
