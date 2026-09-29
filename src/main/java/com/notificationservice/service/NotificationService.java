package com.notificationservice.service;

import com.notificationservice.domain.*;
import com.notificationservice.dto.request.SendNotificationRequest;
import com.notificationservice.dto.request.BulkNotificationRequest;
import com.notificationservice.dto.response.NotificationDetailResponse;
import com.notificationservice.dto.response.NotificationResponse;
import com.notificationservice.pipeline.NotificationContext;
import com.notificationservice.pipeline.NotificationPipeline;
import com.notificationservice.repository.DeliveryAttemptRepository;
import com.notificationservice.repository.NotificationRequestRepository;
import com.notificationservice.exception.TenantNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/**
 * Facade Pattern — single entry point for all notification operations.
 * Hides the pipeline, rate limiter, dispatcher, and idempotency complexity from controllers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationPipeline pipeline;
    private final NotificationRequestRepository requestRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;

    /**
     * Send or schedule a single notification.
     * Runs through the full processing pipeline.
     */
    @Transactional
    public NotificationResponse send(UUID tenantId, UUID userId, SendNotificationRequest req) {
        NotificationContext context = NotificationContext.builder()
                .tenantId(tenantId)
                .templateId(req.getTemplateId())
                .recipientAddress(req.getRecipientAddress())
                .channel(req.getChannel())
                .priority(req.getPriority() != null ? req.getPriority() : Priority.NORMAL)
                .variables(req.getVariables())
                .scheduledAt(req.getScheduledAt())
                .idempotencyKey(req.getIdempotencyKey())
                .createdBy(userId)
                .build();

        pipeline.execute(context);

        // If duplicate, return existing notification
        if (context.isDuplicate()) {
            return requestRepository.findById(context.getExistingNotificationId())
                    .map(existing -> {
                        NotificationResponse resp = NotificationResponse.from(existing);
                        resp.setDuplicate(true);
                        return resp;
                    })
                    .orElse(NotificationResponse.builder()
                            .id(context.getExistingNotificationId())
                            .isDuplicate(true)
                            .build());
        }

        return NotificationResponse.from(context.getSavedRequest());
    }

    /**
     * Bulk send — each notification goes through the pipeline independently.
     */
    @Transactional
    public List<NotificationResponse> sendBulk(UUID tenantId, UUID userId, BulkNotificationRequest req) {
        return req.getNotifications().stream()
                .map(n -> send(tenantId, userId, n))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(UUID tenantId, NotificationStatus status, Pageable pageable) {
        Page<NotificationRequest> page = status != null
                ? requestRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : requestRepository.findByTenantId(tenantId, pageable);
        return page.map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse getDetail(UUID tenantId, UUID notificationId) {
        NotificationRequest request = requestRepository.findByIdAndTenantId(notificationId, tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Notification not found: " + notificationId));

        List<NotificationDetailResponse.DeliveryAttemptResponse> history =
                deliveryAttemptRepository
                        .findByNotificationRequestIdOrderByAttemptNumberAsc(notificationId)
                        .stream()
                        .map(NotificationDetailResponse.DeliveryAttemptResponse::from)
                        .toList();

        return NotificationDetailResponse.builder()
                .id(request.getId())
                .tenantId(request.getTenantId())
                .templateId(request.getTemplateId())
                .recipientAddress(request.getRecipientAddress())
                .channel(request.getChannel())
                .priority(request.getPriority())
                .status(request.getStatus())
                .idempotencyKey(request.getIdempotencyKey())
                .scheduledAt(request.getScheduledAt())
                .renderedSubject(request.getRenderedSubject())
                .renderedBody(request.getRenderedBody())
                .attemptCount(request.getAttemptCount())
                .maxAttempts(request.getMaxAttempts())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .deliveryHistory(history)
                .build();
    }

    @Transactional
    public void cancel(UUID tenantId, UUID notificationId) {
        NotificationRequest request = requestRepository.findByIdAndTenantId(notificationId, tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Notification not found: " + notificationId));

        request.transitionTo(NotificationStatus.CANCELLED);
        requestRepository.save(request);
        log.info("Notification cancelled: id={}", notificationId);
    }
}
