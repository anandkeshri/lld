package org.example.rateLimitter;

public interface RateLimitter {
    boolean isAllowed(String key);
}
