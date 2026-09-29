package com.notificationservice.channel;

import com.notificationservice.domain.*;
import com.notificationservice.event.NotificationDeliveredEvent;
import com.notificationservice.event.NotificationFailedEvent;
import com.notificationservice.repository.DeliveryAttemptRepository;
import com.notificationservice.repository.NotificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import java.time.Instant;

/**
 * Decorator Pattern — wraps any ChannelDispatcher with retry logic,
 * delivery attempt recording, and domain event publishing.
 *
 * The decorated dispatcher handles the actual send; this class handles:
 * - Recording DeliveryAttempt (SUCCESS or FAILURE)
 * - Scheduling exponential backoff retry
 * - Transitioning NotificationRequest status
 * - Publishing domain events (Observer pattern)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RetryAwareChannelDispatcher {

    private final DeliveryAttemptRepository attemptRepository;
    private final NotificationRequestRepository requestRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${notification.dispatch.retry-backoff-base-seconds:30}")
    private int retryBackoffBaseSeconds;

    /**
     * Dispatch with retry recording. Called by NotificationDispatchCommand.
     */
    public void dispatch(NotificationRequest request, ChannelDispatcher delegate) {
        int attemptNumber = request.getAttemptCount() + 1;
        request.setAttemptCount(attemptNumber);
        request.transitionTo(NotificationStatus.DISPATCHED);
        requestRepository.save(request);

        DeliveryAttempt attempt = DeliveryAttempt.builder()
                .notificationRequestId(request.getId())
                .attemptNumber(attemptNumber)
                .status(AttemptStatus.IN_PROGRESS)
                .build();
        attempt = attemptRepository.save(attempt);

        try {
            delegate.dispatch(request);

            // Success
            attempt.setStatus(AttemptStatus.SUCCESS);
            attempt.setProviderResponse("Delivered successfully");
            attemptRepository.save(attempt);

            request.transitionTo(NotificationStatus.DELIVERED);
            requestRepository.save(request);

            eventPublisher.publishEvent(new NotificationDeliveredEvent(this, request));
            log.info("Delivered notificationId={} attempt={}", request.getId(), attemptNumber);

        } catch (Exception ex) {
            // Failure
            attempt.setStatus(AttemptStatus.FAILURE);
            attempt.setErrorMessage(ex.getMessage());

            boolean hasRetries = request.hasRetriesRemaining();
            if (hasRetries) {
                long backoffSeconds = (long) retryBackoffBaseSeconds * (1L << (attemptNumber - 1));
                Instant nextRetry = Instant.now().plusSeconds(backoffSeconds);
                attempt.setNextRetryAt(nextRetry);
                attemptRepository.save(attempt);

                request.transitionTo(NotificationStatus.QUEUED);
                requestRepository.save(request);

                log.warn("Attempt {} failed for notificationId={}. Retry at {} (backoff={}s)",
                        attemptNumber, request.getId(), nextRetry, backoffSeconds);
            } else {
                attemptRepository.save(attempt);
                request.transitionTo(NotificationStatus.FAILED);
                requestRepository.save(request);

                eventPublisher.publishEvent(new NotificationFailedEvent(this, request, ex.getMessage()));
                log.error("All {} attempts exhausted for notificationId={}. Marking FAILED.",
                        attemptNumber, request.getId());
            }
        }
    }
}
