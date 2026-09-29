package com.notificationservice.pipeline;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.Priority;
import com.notificationservice.domain.TenantRateLimitConfig;
import com.notificationservice.exception.RateLimitExceededException;
import com.notificationservice.ratelimit.BucketRateLimiter;
import com.notificationservice.ratelimit.RateLimitResult;
import com.notificationservice.repository.TenantRateLimitConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * Handler 4 — Per-tenant rate limiting using Bucket4j.
 *
 * Resolution order (most specific wins):
 *   1. tenant + channel + priority
 *   2. tenant + channel (any priority)
 *   3. tenant global (any channel, any priority)
 *   4. application default
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitHandler implements NotificationHandler {

    private final BucketRateLimiter rateLimiter;
    private final TenantRateLimitConfigRepository rateLimitConfigRepository;

    @Value("${notification.rate-limit.default-per-minute:100}")
    private int defaultPerMinute;

    @Override
    public void handle(NotificationContext context, NotificationHandlerChain chain) {
        UUID tenantId  = context.getTenantId();
        Channel channel = context.getChannel();
        Priority priority = context.getPriority();

        TenantRateLimitConfig config = resolveConfig(tenantId, channel, priority);
        int limit = config != null ? config.getLimitPerMinute() : defaultPerMinute;
        int burst  = config != null ? config.getBurstCapacity() : defaultPerMinute * 2;

        String bucketKey = buildBucketKey(tenantId, channel, priority);
        RateLimitResult result = rateLimiter.tryConsume(bucketKey, limit, burst);

        if (!result.allowed()) {
            log.warn("Rate limit exceeded for tenant={} channel={} retryAfter={}s",
                    tenantId, channel, result.retryAfterSeconds());
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }

        log.debug("Rate limit OK for tenant={} remaining={}", tenantId, result.remainingTokens());
        chain.next(context);
    }

    @Override
    public int getOrder() { return 4; }

    private TenantRateLimitConfig resolveConfig(UUID tenantId, Channel channel, Priority priority) {
        // 1. Most specific: channel + priority
        return rateLimitConfigRepository
                .findByTenantIdAndChannelAndPriority(tenantId, channel, priority)
                // 2. Channel-level
                .or(() -> rateLimitConfigRepository
                        .findByTenantIdAndChannelAndPriorityIsNull(tenantId, channel))
                // 3. Global tenant
                .or(() -> rateLimitConfigRepository
                        .findByTenantIdAndChannelIsNullAndPriorityIsNull(tenantId))
                .orElse(null);
    }

    private String buildBucketKey(UUID tenantId, Channel channel, Priority priority) {
        return tenantId + ":" + channel + ":" + priority;
    }
}
