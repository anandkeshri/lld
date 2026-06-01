package org.example.miq.validator;

public class LunhValidator implements CardValidator{
    @Override
    public boolean validate(String card) {

        //step -1
       int sum = 0;
        for( int i = 14;i>=0; i = i-2){
            int digit = Integer.parseInt(String.valueOf(card.charAt(i)));
            digit = 2*digit;

            while(digit>9){
                digit = (digit%10) + (digit/10);
            }

            sum = sum + digit;

        }
        int sumStep2 = 0;
        for(int i=15;i>=0;i=i-2){
            int digit = Integer.parseInt(String.valueOf(card.charAt(i)));
            sumStep2 = sumStep2 + digit;
        }

        if((sum+sumStep2)%10 == 0){
            return true;
        }

        return false;
    }
}
