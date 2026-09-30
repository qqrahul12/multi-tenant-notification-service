package com.notificationservice.pipeline;

import com.notificationservice.exception.TenantSuspendedException;
import com.notificationservice.domain.TenantStatus;
import org.springframework.stereotype.Component;

/**
 * Handler 2 — Rejects all sends for suspended tenants.
 */
@Component
public class TenantStatusHandler implements NotificationHandler {

    @Override
    public void handle(NotificationContext context, NotificationHandlerChain chain) {
        if (context.getTenant().getStatus() == TenantStatus.SUSPENDED) {
            throw new TenantSuspendedException(context.getTenantId().toString());
        }
        chain.next(context);
    }

    @Override
    public int getOrder() { return 2; }
}
