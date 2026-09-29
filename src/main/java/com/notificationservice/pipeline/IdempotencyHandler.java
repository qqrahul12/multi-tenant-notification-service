package com.notificationservice.pipeline;

import com.notificationservice.idempotency.IdempotencyStore;
import com.notificationservice.repository.NotificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.Optional;
import java.util.UUID;

/**
 * Handler 3 — Idempotency check using Redis SET NX.
 *
 * If the idempotency key is already claimed in Redis:
 *   - Sets context.duplicate = true
 *   - Sets context.existingNotificationId
 *   - Short-circuits the pipeline (does NOT call chain.next)
 *
 * If key is new: claims it in Redis and proceeds.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyHandler implements NotificationHandler {

    private final IdempotencyStore idempotencyStore;

    @Override
    public void handle(NotificationContext context, NotificationHandlerChain chain) {
        String idempotencyKey = context.getIdempotencyKey();

        if (!StringUtils.hasText(idempotencyKey)) {
            // No idempotency key provided — proceed without dedup check
            chain.next(context);
            return;
        }

        String redisKey = buildRedisKey(context.getTenantId(), idempotencyKey);
        Optional<UUID> existing = idempotencyStore.get(redisKey);

        if (existing.isPresent()) {
            // Already processed — short-circuit, return existing result
            log.info("Duplicate request detected for idempotency key: {}", idempotencyKey);
            context.setDuplicate(true);
            context.setExistingNotificationId(existing.get());
            // Do NOT call chain.next() — pipeline stops here
            return;
        }

        // Claim a placeholder UUID (will be updated with real ID after persist)
        UUID tempId = UUID.randomUUID();
        boolean claimed = idempotencyStore.claim(redisKey, tempId);

        if (!claimed) {
            // Race condition — another request claimed it simultaneously
            Optional<UUID> raceExisting = idempotencyStore.get(redisKey);
            context.setDuplicate(true);
            context.setExistingNotificationId(raceExisting.orElse(null));
            return;
        }

        chain.next(context);

        // After pipeline: update Redis with the real notification ID
        if (context.getSavedRequest() != null) {
            idempotencyStore.release(redisKey);
            idempotencyStore.claim(redisKey, context.getSavedRequest().getId());
        }
    }

    @Override
    public int getOrder() { return 3; }

    private String buildRedisKey(UUID tenantId, String key) {
        return tenantId + ":" + key;
    }
}
