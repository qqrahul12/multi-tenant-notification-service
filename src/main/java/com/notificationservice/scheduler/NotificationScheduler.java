package com.notificationservice.scheduler;

import com.notificationservice.channel.ChannelDispatcherFactory;
import com.notificationservice.channel.RetryAwareChannelDispatcher;
import com.notificationservice.command.NotificationDispatchCommand;
import com.notificationservice.domain.NotificationRequest;
import com.notificationservice.domain.NotificationStatus;
import com.notificationservice.repository.NotificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Background scheduler for:
 * 1. Dispatching PENDING notifications whose scheduledAt has arrived
 * 2. Re-dispatching QUEUED notifications due for retry
 *
 * Uses PostgreSQL SKIP LOCKED (in repository queries) to prevent duplicate
 * dispatch when multiple instances run concurrently — no external lock needed.
 */
@Component
@Slf4j
public class NotificationScheduler {

    private final NotificationRequestRepository requestRepository;
    private final ChannelDispatcherFactory dispatcherFactory;
    private final RetryAwareChannelDispatcher retryAwareDispatcher;
    private final ExecutorService channelDispatchExecutor;
    private final int batchSize;

    public NotificationScheduler(
            NotificationRequestRepository requestRepository,
            ChannelDispatcherFactory dispatcherFactory,
            RetryAwareChannelDispatcher retryAwareDispatcher,
            @Qualifier("channelDispatchExecutor") ExecutorService channelDispatchExecutor,
            @Value("${notification.scheduler.batch-size:100}") int batchSize) {
        this.requestRepository    = requestRepository;
        this.dispatcherFactory    = dispatcherFactory;
        this.retryAwareDispatcher = retryAwareDispatcher;
        this.channelDispatchExecutor = channelDispatchExecutor;
        this.batchSize = batchSize;
    }

    /**
     * Poll for scheduled notifications due for dispatch.
     * fixedDelay ensures the next run starts after the previous finishes (not overlapping).
     */
    @Scheduled(fixedDelayString = "${notification.scheduler.poll-interval-ms:30000}")
    @Transactional
    public void dispatchScheduledNotifications() {
        Instant now = Instant.now();
        List<NotificationRequest> pending = requestRepository.findPendingForDispatch(now, batchSize);

        if (!pending.isEmpty()) {
            log.info("Scheduler: found {} pending notifications to dispatch", pending.size());
        }

        for (NotificationRequest request : pending) {
            try {
                request.transitionTo(NotificationStatus.QUEUED);
                requestRepository.save(request);
                submitForDispatch(request);
            } catch (Exception e) {
                log.error("Scheduler: failed to submit notificationId={}: {}",
                        request.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Poll for QUEUED notifications with failed attempts due for retry.
     */
    @Scheduled(fixedDelayString = "${notification.scheduler.poll-interval-ms:30000}")
    @Transactional
    public void retryFailedNotifications() {
        Instant now = Instant.now();
        List<NotificationRequest> dueForRetry = requestRepository.findDueForRetry(now, batchSize);

        if (!dueForRetry.isEmpty()) {
            log.info("Scheduler: found {} notifications due for retry", dueForRetry.size());
        }

        for (NotificationRequest request : dueForRetry) {
            try {
                submitForDispatch(request);
            } catch (Exception e) {
                log.error("Scheduler: failed to re-submit notificationId={}: {}",
                        request.getId(), e.getMessage(), e);
            }
        }
    }

    private void submitForDispatch(NotificationRequest request) {
        var command = new NotificationDispatchCommand(
                request,
                dispatcherFactory.getDispatcher(request.getChannel()),
                retryAwareDispatcher);
        channelDispatchExecutor.submit(command);
        log.debug("Scheduler: submitted notificationId={}", request.getId());
    }
}
