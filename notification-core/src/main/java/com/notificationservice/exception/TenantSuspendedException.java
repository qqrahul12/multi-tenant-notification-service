package com.notificationservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TenantSuspendedException extends RuntimeException {
    public TenantSuspendedException(String tenantId) {
        super("Tenant '" + tenantId + "' is suspended. Notifications cannot be sent.");
    }
}
