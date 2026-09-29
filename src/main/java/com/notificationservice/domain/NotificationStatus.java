package com.notificationservice.domain;

import com.notificationservice.exception.InvalidStateTransitionException;
import java.util.Map;
import java.util.Set;

/**
 * State machine for notification lifecycle.
 * Pattern: State — guards illegal transitions, documents valid flow.
 *
 *  PENDING ──► QUEUED ──► DISPATCHED ──► DELIVERED
 *    │            │             │
 *    │            │             └──► QUEUED (retry scheduled)
 *    │            │             └──► FAILED (max retries)
 *    └──► CANCELLED ◄── QUEUED
 */
public enum NotificationStatus {
    PENDING, QUEUED, DISPATCHED, DELIVERED, FAILED, CANCELLED;

    private static final Map<NotificationStatus, Set<NotificationStatus>> VALID_TRANSITIONS = Map.of(
        PENDING,    Set.of(QUEUED, CANCELLED),
        QUEUED,     Set.of(DISPATCHED, CANCELLED, FAILED),
        DISPATCHED, Set.of(DELIVERED, QUEUED, FAILED),
        DELIVERED,  Set.of(),   // terminal
        FAILED,     Set.of(),   // terminal
        CANCELLED,  Set.of()    // terminal
    );

    public boolean canTransitionTo(NotificationStatus next) {
        return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }

    public void assertCanTransitionTo(NotificationStatus next) {
        if (!canTransitionTo(next)) {
            throw new InvalidStateTransitionException(
                String.format("Cannot transition from %s to %s", this, next));
        }
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == FAILED || this == CANCELLED;
    }
}
