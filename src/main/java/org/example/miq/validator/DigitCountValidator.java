package org.example.miq.validator;

public class DigitCountValidator implements CardValidator{

    int CARDDIGIT = 16;
    @Override
    public boolean validate(String card) {
        if (card.length()!=CARDDIGIT){
            return false;
        }

        for(char ch : card.toCharArray()){
            if(ch>'9' || ch<'0'){
                return false;
            }
        }
        return true;
    }
}
