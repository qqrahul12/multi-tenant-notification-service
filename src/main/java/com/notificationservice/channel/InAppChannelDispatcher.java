package com.notificationservice.channel;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Random;

@Component
@Slf4j
public class InAppChannelDispatcher extends AbstractChannelDispatcher {

    @Value("${notification.channel.in-app.failure-rate:0.01}")
    private double failureRate;

    private final Random random = new Random();

    @Override
    protected String doSend(NotificationRequest request) {
        if (random.nextDouble() < failureRate) {
            throw new TransientChannelException("[IN_APP] Simulated in-app delivery failure");
        }
        // In-app: write to the user's notification inbox (DB/WebSocket)
        log.info("[IN_APP] Delivered to userId={} body={}",
                request.getRecipientAddress(),
                truncate(request.getRenderedBody(), 50));
        return "IN_APP_DELIVERED_AT_" + System.currentTimeMillis();
    }

    @Override
    public Channel getSupportedChannel() { return Channel.IN_APP; }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
