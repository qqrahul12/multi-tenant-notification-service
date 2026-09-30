package com.notificationservice.channel;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.NotificationRequest;

/**
 * Strategy Pattern — defines the contract for all channel dispatchers.
 * LSP: all implementations must be substitutable without breaking callers.
 * ISP: only dispatch-related methods.
 */
public interface ChannelDispatcher {

    /**
     * Dispatch the notification to the channel provider.
     * Throws TransientChannelException on recoverable errors (will be retried).
     * Throws PermanentChannelException on non-recoverable errors (no retry).
     */
    void dispatch(NotificationRequest request) throws Exception;

    /**
     * The channel this dispatcher handles.
     * Used by ChannelDispatcherFactory for Strategy selection.
     */
    Channel getSupportedChannel();
}
