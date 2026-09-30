package com.notificationservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notification_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID templateId;

    @Column(nullable = false)
    private String recipientAddress;
    @Column(name = "recipient_id")
    private UUID recipientId;


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> variables = Map.of();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Priority priority = Priority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    private Instant scheduledAt;   // null = immediate send

    private String idempotencyKey;

    @Column(columnDefinition = "TEXT")
    private String renderedSubject;

    @Column(columnDefinition = "TEXT")
    private String renderedBody;

    @Builder.Default
    private int attemptCount = 0;

    @Builder.Default
    private int maxAttempts = 3;

    private UUID createdBy;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    private Instant updatedAt;

    /**
     * Transition status using the State pattern guard.
     */
    public void transitionTo(NotificationStatus newStatus) {
        this.status.assertCanTransitionTo(newStatus);
        this.status = newStatus;
    }

    public boolean isImmediate() {
        return scheduledAt == null;
    }

    public boolean isScheduled() {
        return scheduledAt != null;
    }

    public boolean isReadyToDispatch() {
        return scheduledAt == null || !Instant.now().isBefore(scheduledAt);
    }

    public boolean hasRetriesRemaining() {
        return attemptCount < maxAttempts;
    }
}
