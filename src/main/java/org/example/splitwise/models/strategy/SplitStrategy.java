package org.example.splitwise.models.strategy;

import org.example.splitwise.errors.InvalidSplit;
import org.example.splitwise.models.User;

import java.util.Map;

public interface SplitStrategy {
    Map<User, Double> split(User paidBy, Double totalAmount, Map<User, Double> userShare) throws InvalidSplit;
    boolean isValidSplit(User paidBy, Double totalAmount, Map<User, Double> userShare);
}
