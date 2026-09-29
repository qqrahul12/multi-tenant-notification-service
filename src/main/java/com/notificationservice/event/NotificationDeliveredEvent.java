package com.notificationservice.event;

import com.notificationservice.domain.NotificationRequest;
import org.springframework.context.ApplicationEvent;

/** Published when a notification is successfully delivered. */
public class NotificationDeliveredEvent extends ApplicationEvent {
    private final NotificationRequest request;

    public NotificationDeliveredEvent(Object source, NotificationRequest request) {
        super(source);
        this.request = request;
    }

    public NotificationRequest getRequest() { return request; }
}
