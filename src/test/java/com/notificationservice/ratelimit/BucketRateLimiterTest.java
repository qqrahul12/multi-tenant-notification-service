package com.notificationservice.ratelimit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BucketRateLimiterTest {

    private final BucketRateLimiter rateLimiter = new BucketRateLimiter();

    @Test
    void shouldAllowUnderLimit() {
        String key = "test-tenant-1:EMAIL:HIGH";
        
        // limit=1, burst=1
        RateLimitResult result1 = rateLimiter.tryConsume(key, 1, 1);
        assertTrue(result1.allowed());
        assertEquals(0, result1.remainingTokens());
        assertEquals(0, result1.retryAfterSeconds());

        // Second request should be denied
        RateLimitResult result2 = rateLimiter.tryConsume(key, 1, 1);
        assertFalse(result2.allowed());
        assertTrue(result2.retryAfterSeconds() > 0);
    }

    @Test
    void shouldRecreateBucketOnInvalidate() {
        String key = "test-tenant-2";
        
        RateLimitResult result1 = rateLimiter.tryConsume(key, 1, 1);
        assertTrue(result1.allowed());

        RateLimitResult result2 = rateLimiter.tryConsume(key, 1, 1);
        assertFalse(result2.allowed()); // Depleted

        // Invalidate bucket
        rateLimiter.invalidateBucket(key);

        // Should be allowed again since a new bucket is created
        RateLimitResult result3 = rateLimiter.tryConsume(key, 1, 1);
        assertTrue(result3.allowed());
    }

    @Test
    void shouldInvalidateAllForTenant() {
        rateLimiter.tryConsume("tenant3:EMAIL", 1, 1);
        rateLimiter.tryConsume("tenant3:SMS", 1, 1);
        
        assertFalse(rateLimiter.tryConsume("tenant3:EMAIL", 1, 1).allowed());
        
        rateLimiter.invalidateAllForTenant("tenant3");
        
        assertTrue(rateLimiter.tryConsume("tenant3:EMAIL", 1, 1).allowed());
        assertTrue(rateLimiter.tryConsume("tenant3:SMS", 1, 1).allowed());
    }
}
