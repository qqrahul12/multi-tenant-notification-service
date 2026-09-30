package com.notificationservice.command;

import com.notificationservice.channel.ChannelDispatcher;
import com.notificationservice.channel.RetryAwareChannelDispatcher;
import com.notificationservice.domain.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

/**
 * Command Pattern — encapsulates a dispatch operation as a Runnable.
 *
 * Submitted to the virtual thread executor. This allows:
 * - Queuing / prioritization
 * - Cancellation (future.cancel())
 * - Decoupling dispatch from calling thread
 *
 * The RetryAwareChannelDispatcher (Decorator) is looked up from Spring context
 * to avoid circular dependency issues.
 */
@Slf4j
@RequiredArgsConstructor
public class NotificationDispatchCommand implements Runnable {

    private final NotificationRequest request;
    private final ChannelDispatcher dispatcher;
    private final RetryAwareChannelDispatcher retryAwareDispatcher;

    @Override
    public void run() {
        log.debug("Executing dispatch command for notificationId={} channel={}",
                request.getId(), request.getChannel());
        try {
            retryAwareDispatcher.dispatch(request, dispatcher);
        } catch (Exception e) {
            log.error("Unhandled error in dispatch command for notificationId={}: {}",
                    request.getId(), e.getMessage(), e);
        }
    }
}
