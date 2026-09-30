package com.notificationservice.pipeline;

import com.notificationservice.channel.ChannelDispatcherFactory;
import com.notificationservice.command.NotificationDispatchCommand;
import com.notificationservice.domain.*;
import com.notificationservice.repository.NotificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.concurrent.ExecutorService;

/**
 * Handler 6 (final) — Persists the NotificationRequest and dispatches it.
 *
 * For immediate sends: submits a NotificationDispatchCommand to the virtual thread executor.
 * For scheduled sends: persists with PENDING status; the scheduler picks it up later.
 */
@Component
@Slf4j
public class DispatchHandler implements NotificationHandler {

    private final NotificationRequestRepository requestRepository;
    private final ChannelDispatcherFactory dispatcherFactory;
    private final ExecutorService channelDispatchExecutor;
    private final com.notificationservice.channel.RetryAwareChannelDispatcher retryAwareDispatcher;

    public DispatchHandler(
            NotificationRequestRepository requestRepository,
            ChannelDispatcherFactory dispatcherFactory,
            @Qualifier("channelDispatchExecutor") ExecutorService channelDispatchExecutor,
            com.notificationservice.channel.RetryAwareChannelDispatcher retryAwareDispatcher) {
        this.requestRepository    = requestRepository;
        this.dispatcherFactory    = dispatcherFactory;
        this.channelDispatchExecutor = channelDispatchExecutor;
        this.retryAwareDispatcher = retryAwareDispatcher;
    }

    @Override
    public void handle(NotificationContext context, NotificationHandlerChain chain) {
        // Persist
        NotificationRequest request = buildRequest(context);
        request = requestRepository.save(request);
        context.setSavedRequest(request);

        log.info("NotificationRequest persisted: id={} channel={} scheduled={}",
                request.getId(), request.getChannel(), request.getScheduledAt());

        // Dispatch
        if (request.isImmediate()) {
            request.transitionTo(NotificationStatus.QUEUED);
            final NotificationRequest finalRequest = requestRepository.save(request);
            
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        submitForDispatch(finalRequest);
                    }
                });
            } else {
                submitForDispatch(finalRequest);
            }
        }
        // Scheduled: leave as PENDING — scheduler will pick it up

        chain.next(context);
    }

    @Override
    public int getOrder() { return 6; }

    private NotificationRequest buildRequest(NotificationContext ctx) {
        Instant scheduledAt = ctx.getScheduledAt() != null
                ? Instant.parse(ctx.getScheduledAt()) : null;

        return NotificationRequest.builder()
                .tenantId(ctx.getTenantId())
                .templateId(ctx.getTemplateId())
                .recipientId(ctx.getRecipientId())

                .recipientAddress(ctx.getRecipientAddress())
                .variables(ctx.getVariables())
                .channel(ctx.getChannel())
                .priority(ctx.getPriority() != null ? ctx.getPriority() : Priority.NORMAL)
                .scheduledAt(scheduledAt)
                .idempotencyKey(ctx.getIdempotencyKey())
                .renderedSubject(ctx.getRenderedSubject())
                .renderedBody(ctx.getRenderedBody())
                .createdBy(ctx.getCreatedBy())
                .build();
    }

    private void submitForDispatch(NotificationRequest request) {
        NotificationDispatchCommand command = new NotificationDispatchCommand(
                request, 
                dispatcherFactory.getDispatcher(request.getChannel()),
                retryAwareDispatcher);
        channelDispatchExecutor.submit(command);
        log.debug("Submitted dispatch command for notificationId={}", request.getId());
    }
}
