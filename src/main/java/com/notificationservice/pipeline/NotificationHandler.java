package com.notificationservice.pipeline;

/**
 * Chain of Responsibility — each handler processes the context and
 * decides whether to pass to the next handler or short-circuit.
 */
public interface NotificationHandler {

    /**
     * Process the context.
     * Call chain.next(context) to pass to the next handler.
     * Throw an exception to abort the pipeline.
     */
    void handle(NotificationContext context, NotificationHandlerChain chain);

    /**
     * Order in the pipeline (lower = earlier).
     */
    int getOrder();
}
