package com.notificationservice.pipeline;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;

/**
 * Facade over the pipeline — assembles handlers in order and executes them.
 * New handlers just need to implement NotificationHandler and be @Component.
 * OCP: adding a new handler requires zero changes here.
 */
@Component
@RequiredArgsConstructor
public class NotificationPipeline {

    private final List<NotificationHandler> handlers;

    public void execute(NotificationContext context) {
        List<NotificationHandler> orderedHandlers = handlers.stream()
                .sorted(Comparator.comparingInt(NotificationHandler::getOrder))
                .toList();
        new NotificationHandlerChain(orderedHandlers).next(context);
    }
}
