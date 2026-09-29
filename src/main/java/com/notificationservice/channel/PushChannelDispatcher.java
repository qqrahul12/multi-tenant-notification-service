package com.notificationservice.channel;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Random;

@Component
@Slf4j
public class PushChannelDispatcher extends AbstractChannelDispatcher {

    @Value("${notification.channel.push.failure-rate:0.05}")
    private double failureRate;

    private final Random random = new Random();

    @Override
    protected String doSend(NotificationRequest request) {
        if (random.nextDouble() < failureRate) {
            throw new TransientChannelException("[PUSH] Simulated FCM/APNs transient failure");
        }
        simulateNetworkLatency(30, 150);
        log.info("[PUSH] Sent to device={} title={}", request.getRecipientAddress(),
                request.getRenderedSubject());
        return "PUSH_PROVIDER_MSG_ID_" + System.currentTimeMillis();
    }

    @Override
    public Channel getSupportedChannel() { return Channel.PUSH; }

    private void simulateNetworkLatency(int minMs, int maxMs) {
        try { Thread.sleep(minMs + random.nextInt(maxMs - minMs)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
