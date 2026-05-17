package org.example.rateLimitter.strategies;

public interface RateLimitStrategy {
    boolean isAllowed(String key);
}
