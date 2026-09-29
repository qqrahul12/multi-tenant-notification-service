package com.notificationservice.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token bucket rate limiter using Bucket4j (in-memory).
 *
 * Strategy Pattern: implements RateLimiter interface — swappable with a
 * Redis-backed implementation in production for distributed rate limiting.
 *
 * Each tenant gets its own bucket keyed by "tenantId[:channel][:priority]".
 * Bucket config is refreshed when the tenant's rate limit config changes
 * (buckets are removed from the map — recreated on next request).
 */
@Component
public class BucketRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult tryConsume(String bucketKey, int limit, int burst) {
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(limit, burst));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return RateLimitResult.allowed(probe.getRemainingTokens());
        } else {
            long nanosToWait = probe.getNanosToWaitForRefill();
            return RateLimitResult.denied(Duration.ofNanos(nanosToWait));
        }
    }

    /**
     * Invalidate a bucket when its config changes so it gets recreated
     * with the new limits on next request.
     */
    public void invalidateBucket(String bucketKey) {
        buckets.remove(bucketKey);
    }

    public void invalidateAllForTenant(String tenantId) {
        buckets.keySet().removeIf(k -> k.startsWith(tenantId));
    }

    private Bucket createBucket(int limitPerMinute, int burst) {
        Bandwidth refillBandwidth = Bandwidth.builder()
                .capacity(burst)
                .refillGreedy(limitPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(refillBandwidth)
                .build();
    }
}
