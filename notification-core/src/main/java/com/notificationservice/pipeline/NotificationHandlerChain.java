package com.notificationservice.pipeline;

import java.util.List;

/**
 * Iterates through the ordered list of handlers.
 * Each handler calls chain.next() to proceed, or throws to abort.
 */
public class NotificationHandlerChain {

    private final List<NotificationHandler> handlers;
    private int index = 0;

    public NotificationHandlerChain(List<NotificationHandler> handlers) {
        this.handlers = handlers;
    }

    public void next(NotificationContext context) {
        if (index < handlers.size()) {
            NotificationHandler handler = handlers.get(index++);
            handler.handle(context, this);
        }
    }
}
