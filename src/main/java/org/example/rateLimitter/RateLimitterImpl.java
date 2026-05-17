package org.example.rateLimitter;

import org.example.rateLimitter.strategies.RateLimitStrategy;

public class RateLimitterImpl implements RateLimitter{
    private final RateLimitStrategy strategy;


    public RateLimitterImpl( RateLimitStrategy strategy){
        this.strategy = strategy;
    }

    @Override
    public boolean isAllowed(String key) {
        return strategy.isAllowed(key);
    }
}
