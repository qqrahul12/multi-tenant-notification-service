package com.notificationservice.event;

import com.notificationservice.domain.AuditLog;
import com.notificationservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Observer Pattern — listens to domain events and writes audit log entries.
 *
 * @Async means audit logging runs on a virtual thread — never blocks the dispatch thread.
 * Decoupled from core delivery logic (OCP — add new events without changing dispatchers).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogEventListener {

    private final AuditLogRepository auditLogRepository;

    @EventListener
    @Async
    public void onDelivered(NotificationDeliveredEvent event) {
        var request = event.getRequest();
        auditLogRepository.save(AuditLog.builder()
                .tenantId(request.getTenantId())
                .entityType("NotificationRequest")
                .entityId(request.getId())
                .action("DELIVERED")
                .newState(Map.of(
                        "status", "DELIVERED",
                        "attemptCount", String.valueOf(request.getAttemptCount()),
                        "channel", request.getChannel().name()
                ))
                .build());
        log.debug("Audit: DELIVERED notificationId={}", request.getId());
    }

    @EventListener
    @Async
    public void onFailed(NotificationFailedEvent event) {
        var request = event.getRequest();
        auditLogRepository.save(AuditLog.builder()
                .tenantId(request.getTenantId())
                .entityType("NotificationRequest")
                .entityId(request.getId())
                .action("FAILED")
                .newState(Map.of(
                        "status", "FAILED",
                        "attemptCount", String.valueOf(request.getAttemptCount()),
                        "errorMessage", event.getErrorMessage()
                ))
                .build());
        log.debug("Audit: FAILED notificationId={}", request.getId());
    }
}
