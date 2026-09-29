package com.notificationservice.pipeline;

import com.notificationservice.domain.*;
import lombok.Builder;
import lombok.Data;
import java.util.Map;
import java.util.UUID;

/**
 * Shared context object flowing through the notification processing pipeline.
 * Each handler can read/write context — avoids passing many params between handlers.
 */
@Data
@Builder
public class NotificationContext {

    // Input
    private UUID tenantId;
    private UUID templateId;
    private String recipientAddress;
    private Map<String, String> variables;
    private Channel channel;
    private Priority priority;
    private String scheduledAt;           // ISO 8601, null = immediate
    private String idempotencyKey;        // optional
    private UUID createdBy;

    // Enriched by pipeline
    private Tenant tenant;
    private NotificationTemplate template;
    private NotificationChannelConfig channelConfig;
    private String renderedSubject;
    private String renderedBody;
    private NotificationRequest savedRequest;

    // Control flags
    private boolean duplicate;            // set by IdempotencyHandler if already processed
    private UUID existingNotificationId;  // set when duplicate = true
}
