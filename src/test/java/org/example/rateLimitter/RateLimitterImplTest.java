package org.example.rateLimitter;

import org.example.rateLimitter.strategies.FixedTimeWindow;
import org.example.rateLimitter.strategies.RateLimitStrategy;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitterImplTest {

    @Test
    void isAllowed() throws InterruptedException {
        Config config = new Config(4, 1, TimeUnit.SEC);
        RateLimitStrategy strategy = new FixedTimeWindow(config);
        RateLimitter rateLimitter = new RateLimitterImpl(strategy);

        int threadCount = 100;
        // 1. Initialize the fixed thread pool
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger allowedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for the "Go" signal
                    if (rateLimitter.isAllowed("key1")) {
                        allowedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads at once
        doneLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);

        assertEquals(4, allowedCount.get(), "All 4 requests should be allowed");

        assertTrue(true);
    }
}