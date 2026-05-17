package org.example.splitwise.models.strategy;

import org.example.splitwise.errors.InvalidSplit;
import org.example.splitwise.models.User;

import java.util.HashMap;
import java.util.Map;

public class PercentageSplit implements SplitStrategy{
    @Override
    public Map<User, Double> split(User paidBy, Double totalAmount, Map<User, Double> userShare) throws InvalidSplit {
        if(!isValidSplit(paidBy, totalAmount, userShare)){
            throw new InvalidSplit("split id invalid");
        }
        User diffBearer=null;
        Map<User, Double> userAmount = new HashMap<>();
        double currAmount = 0.0;
        for(Map.Entry<User, Double> e : userShare.entrySet()){
            if(diffBearer==null){
                diffBearer = e.getKey();
            }
            double share = (totalAmount*e.getValue())/100.0;
            currAmount = currAmount + share;
            userAmount.put(e.getKey(), share);
        }
        userAmount.put(diffBearer, userAmount.get(diffBearer) + (totalAmount-currAmount));
        return userAmount;
    }

    @Override
    public boolean isValidSplit(User paidBy, Double totalAmount, Map<User, Double> userShare){
        double total = 0.0;
        for(Map.Entry<User, Double> e : userShare.entrySet()){
            total = total+e.getValue();
        }
        return total == 100.0;
    }
}
