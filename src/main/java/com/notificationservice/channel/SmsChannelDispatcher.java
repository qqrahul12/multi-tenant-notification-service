package com.notificationservice.channel;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Random;

@Component
@Slf4j
public class SmsChannelDispatcher extends AbstractChannelDispatcher {

    @Value("${notification.channel.sms.failure-rate:0.05}")
    private double failureRate;

    private final Random random = new Random();

    @Override
    protected String doSend(NotificationRequest request) {
        if (random.nextDouble() < failureRate) {
            throw new TransientChannelException("[SMS] Simulated transient failure (provider unavailable)");
        }
        simulateNetworkLatency(50, 200);
        log.info("[SMS] Sent to={} body={}", request.getRecipientAddress(),
                truncate(request.getRenderedBody(), 50));
        return "SMS_PROVIDER_SID_" + System.currentTimeMillis();
    }

    @Override
    public Channel getSupportedChannel() { return Channel.SMS; }

    private void simulateNetworkLatency(int minMs, int maxMs) {
        try { Thread.sleep(minMs + random.nextInt(maxMs - minMs)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
