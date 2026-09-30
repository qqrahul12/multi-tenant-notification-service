package com.notificationservice.dto.request;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.Priority;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;
import java.util.UUID;

@Data
public class SendNotificationRequest {

    @NotNull(message = "templateId is required")
    private UUID templateId;

    @NotNull(message = "recipientId is required")
    private UUID recipientId;

    @NotNull(message = "channel is required")
    private Channel channel;

    private Priority priority = Priority.NORMAL;

    private Map<String, String> variables;

    private String scheduledAt;
    private String idempotencyKey;
}
