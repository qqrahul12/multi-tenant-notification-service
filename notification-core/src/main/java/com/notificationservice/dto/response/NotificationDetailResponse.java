package com.notificationservice.dto.response;

import com.notificationservice.domain.*;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class NotificationDetailResponse {
    private UUID id;
    private UUID tenantId;
    private UUID templateId;
    private UUID recipientId;

    private String recipientAddress;
    private Channel channel;
    private Priority priority;
    private NotificationStatus status;
    private String idempotencyKey;
    private Instant scheduledAt;
    private String renderedSubject;
    private String renderedBody;
    private int attemptCount;
    private int maxAttempts;
    private Instant createdAt;
    private Instant updatedAt;
    private List<DeliveryAttemptResponse> deliveryHistory;

    public static NotificationDetailResponse from(NotificationRequest r, List<DeliveryAttempt> attempts) {
        return NotificationDetailResponse.builder()
                .id(r.getId())
                .tenantId(r.getTenantId())
                .templateId(r.getTemplateId())
                .recipientId(r.getRecipientId())
                .recipientAddress(r.getRecipientAddress())
                .channel(r.getChannel())
                .priority(r.getPriority())
                .status(r.getStatus())
                .idempotencyKey(r.getIdempotencyKey())
                .scheduledAt(r.getScheduledAt())
                .renderedSubject(r.getRenderedSubject())
                .renderedBody(r.getRenderedBody())
                .attemptCount(r.getAttemptCount())
                .maxAttempts(r.getMaxAttempts())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .deliveryHistory(attempts.stream().map(DeliveryAttemptResponse::from).toList())
                .build();
    }

    @Data
    @Builder
    public static class DeliveryAttemptResponse {
        private UUID id;
        private int attemptNumber;
        private AttemptStatus status;
        private String errorCode;
        private String errorMessage;
        private String providerResponse;
        private Instant nextRetryAt;
        private Instant attemptedAt;

        public static DeliveryAttemptResponse from(DeliveryAttempt a) {
            return DeliveryAttemptResponse.builder()
                    .id(a.getId())
                    .attemptNumber(a.getAttemptNumber())
                    .status(a.getStatus())
                    .errorCode(a.getErrorCode())
                    .errorMessage(a.getErrorMessage())
                    .providerResponse(a.getProviderResponse())
                    .nextRetryAt(a.getNextRetryAt())
                    .attemptedAt(a.getAttemptedAt())
                    .build();
        }
    }
}
