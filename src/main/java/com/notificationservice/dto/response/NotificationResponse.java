package com.notificationservice.dto.response;

import com.notificationservice.domain.*;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private UUID tenantId;
    private UUID templateId;
    private String recipientAddress;
    private Channel channel;
    private Priority priority;
    private NotificationStatus status;
    private String idempotencyKey;
    private Instant scheduledAt;
    private int attemptCount;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isDuplicate;  // true if returned from idempotency cache

    public static NotificationResponse from(NotificationRequest req) {
        return NotificationResponse.builder()
                .id(req.getId())
                .tenantId(req.getTenantId())
                .templateId(req.getTemplateId())
                .recipientAddress(req.getRecipientAddress())
                .channel(req.getChannel())
                .priority(req.getPriority())
                .status(req.getStatus())
                .idempotencyKey(req.getIdempotencyKey())
                .scheduledAt(req.getScheduledAt())
                .attemptCount(req.getAttemptCount())
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .build();
    }
}
