package com.notificationservice.service;

import com.notificationservice.domain.*;
import com.notificationservice.dto.request.BulkNotificationRequest;
import com.notificationservice.dto.request.SendNotificationRequest;
import com.notificationservice.dto.response.NotificationDetailResponse;
import com.notificationservice.dto.response.NotificationResponse;
import com.notificationservice.pipeline.NotificationContext;
import com.notificationservice.pipeline.NotificationPipeline;
import com.notificationservice.repository.DeliveryAttemptRepository;
import com.notificationservice.repository.NotificationRequestRepository;
import com.notificationservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationPipeline pipeline;
    private final NotificationRequestRepository requestRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final UserRepository userRepository;

    public NotificationResponse send(UUID tenantId, UUID userId, SendNotificationRequest req) {
        User recipient = userRepository.findByIdAndTenantId(req.getRecipientId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient user not found or does not belong to tenant"));

        String actualAddress = resolveAddress(recipient, req.getChannel());

        NotificationContext context = NotificationContext.builder()
                .tenantId(tenantId)
                .templateId(req.getTemplateId())
                .recipientId(req.getRecipientId())
                .recipientAddress(actualAddress)
                .channel(req.getChannel())
                .priority(req.getPriority() != null ? req.getPriority() : Priority.NORMAL)
                .variables(req.getVariables())
                .scheduledAt(req.getScheduledAt())
                .idempotencyKey(req.getIdempotencyKey())
                .createdBy(userId)
                .build();

        pipeline.execute(context);

        if (context.isDuplicate()) {
            return requestRepository.findById(context.getExistingNotificationId())
                    .map(existing -> {
                        NotificationResponse resp = NotificationResponse.from(existing);
                        resp.setDuplicate(true);
                        return resp;
                    })
                    .orElseThrow(() -> new IllegalStateException("Duplicate request found but missing in DB"));
        }

        return NotificationResponse.from(context.getSavedRequest());
    }

    public List<NotificationResponse> sendBulk(UUID tenantId, UUID userId, BulkNotificationRequest req) {
        return req.getNotifications().stream()
                .map(r -> {
                    try {
                        return send(tenantId, userId, r);
                    } catch (Exception e) {
                        log.error("Bulk processing failed for one request: {}", e.getMessage());
                        return null;
                    }
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(UUID tenantId, NotificationStatus status, Pageable pageable) {
        if (status != null) {
            return requestRepository.findByTenantIdAndStatus(tenantId, status, pageable).map(NotificationResponse::from);
        }
        return requestRepository.findByTenantId(tenantId, pageable).map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse getDetail(UUID tenantId, UUID notificationId) {
        NotificationRequest request = requestRepository.findByIdAndTenantId(notificationId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        
        List<DeliveryAttempt> attempts = deliveryAttemptRepository.findByNotificationRequestIdOrderByAttemptNumberAsc(notificationId);
        return NotificationDetailResponse.from(request, attempts);
    }

    @Transactional
    public void cancel(UUID tenantId, UUID notificationId) {
        NotificationRequest request = requestRepository.findByIdAndTenantId(notificationId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (request.getStatus() == NotificationStatus.DISPATCHED || request.getStatus() == NotificationStatus.FAILED) {
            throw new IllegalStateException("Cannot cancel a notification in terminal state: " + request.getStatus());
        }

        request.setStatus(NotificationStatus.CANCELLED);
        request.setUpdatedAt(Instant.now());
        requestRepository.save(request);
        log.info("Cancelled notification id={}", notificationId);
    }

    private String resolveAddress(User recipient, Channel channel) {
        switch (channel) {
            case EMAIL:
                if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
                    throw new IllegalArgumentException("Recipient does not have an email address configured");
                }
                return recipient.getEmail();
            case SMS:
                if (recipient.getPhoneNumber() == null || recipient.getPhoneNumber().isBlank()) {
                    throw new IllegalArgumentException("Recipient does not have a phone number configured");
                }
                return recipient.getPhoneNumber();
            case PUSH:
                if (recipient.getDeviceToken() == null || recipient.getDeviceToken().isBlank()) {
                    throw new IllegalArgumentException("Recipient does not have a push device token configured");
                }
                return recipient.getDeviceToken();
            case IN_APP:
                return recipient.getId().toString();
            default:
                throw new IllegalArgumentException("Unsupported channel: " + channel);
        }
    }
}
