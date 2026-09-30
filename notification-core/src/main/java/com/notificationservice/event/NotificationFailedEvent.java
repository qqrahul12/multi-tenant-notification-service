package com.notificationservice.event;

import com.notificationservice.domain.NotificationRequest;
import org.springframework.context.ApplicationEvent;

/** Published when a notification exhausts all retry attempts. */
public class NotificationFailedEvent extends ApplicationEvent {
    private final NotificationRequest request;
    private final String errorMessage;

    public NotificationFailedEvent(Object source, NotificationRequest request, String errorMessage) {
        super(source);
        this.request = request;
        this.errorMessage = errorMessage;
    }

    public NotificationRequest getRequest() { return request; }
    public String getErrorMessage()         { return errorMessage; }
}
