package com.notificationservice.ratelimit;

import java.time.Duration;

/**
 * ISP: thin interface — only what rate limiting needs.
 */
public interface RateLimiter {

    /**
     * Try to consume one token for the given bucket key.
     * @param bucketKey  unique key e.g. "tenantId:channel:priority"
     * @param limit      max requests per minute
     * @param burst      max burst capacity
     */
    RateLimitResult tryConsume(String bucketKey, int limit, int burst);
}
