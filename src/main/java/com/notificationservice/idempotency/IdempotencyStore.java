package com.notificationservice.idempotency;

import java.util.Optional;
import java.util.UUID;

/**
 * ISP: thin interface for idempotency key storage.
 * Redis implementation uses SET NX with TTL.
 */
public interface IdempotencyStore {

    /**
     * Attempt to claim an idempotency key.
     * Returns true if claimed (first time), false if already exists.
     */
    boolean claim(String key, UUID notificationId);

    /**
     * Retrieve the notification ID previously stored for a key.
     */
    Optional<UUID> get(String key);

    /**
     * Release a key (used if the notification was rejected after claim).
     */
    void release(String key);
}
