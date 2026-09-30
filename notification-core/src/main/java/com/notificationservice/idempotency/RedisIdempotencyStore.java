package com.notificationservice.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed idempotency store using SET NX (Set if Not eXists) with 24h TTL.
 *
 * Key format: "idempotency:{tenantId}:{userProvidedKey}"
 * Value:      the NotificationRequest UUID
 *
 * This prevents duplicate deliveries when:
 * - Client retries the same request due to network timeout
 * - Same request is sent twice by mistake
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final Duration TTL = Duration.ofHours(24);
    private static final String PREFIX = "idempotency:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean claim(String key, UUID notificationId) {
        Boolean wasSet = redisTemplate.opsForValue()
                .setIfAbsent(PREFIX + key, notificationId.toString(), TTL);
        boolean claimed = Boolean.TRUE.equals(wasSet);
        if (claimed) {
            log.debug("Idempotency key claimed: {}", key);
        } else {
            log.debug("Idempotency key already exists: {}", key);
        }
        return claimed;
    }

    @Override
    public Optional<UUID> get(String key) {
        Object value = redisTemplate.opsForValue().get(PREFIX + key);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(value.toString()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public void release(String key) {
        redisTemplate.delete(PREFIX + key);
        log.debug("Idempotency key released: {}", key);
    }
}
