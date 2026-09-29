package com.notificationservice.dto.request;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;
import java.util.UUID;

@Data
public class SendNotificationRequest {

    @NotNull(message = "templateId is required")
    private UUID templateId;

    @NotBlank(message = "recipientAddress is required")
    private String recipientAddress;

    @NotNull(message = "channel is required")
    private Channel channel;

    private Priority priority = Priority.NORMAL;

    private Map<String, String> variables;

    /** ISO 8601 datetime. null = send immediately */
    private String scheduledAt;

    /** Optional: client-provided key to prevent duplicate sends */
    private String idempotencyKey;
}
