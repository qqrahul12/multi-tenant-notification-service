package com.notificationservice.channel;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Random;

/**
 * Mock Email channel dispatcher.
 * In production: replace doSend() with real SendGrid/SES/Mailgun API call.
 * Configurable failure rate for testing retry logic.
 */
@Component
@Slf4j
public class EmailChannelDispatcher extends AbstractChannelDispatcher {

    @Value("${notification.channel.email.failure-rate:0.1}")
    private double failureRate;

    private final Random random = new Random();

    @Override
    protected String doSend(NotificationRequest request) {
        simulateFailure("EMAIL");

        // Simulate API call latency (virtual threads park here — no platform thread blocked)
        simulateNetworkLatency(100, 300);

        log.info("[EMAIL] Sent to={} subject={}",
                request.getRecipientAddress(), request.getRenderedSubject());

        return "EMAIL_PROVIDER_MSG_ID_" + System.currentTimeMillis();
    }

    @Override
    public Channel getSupportedChannel() { return Channel.EMAIL; }

    private void simulateFailure(String channel) {
        if (random.nextDouble() < failureRate) {
            throw new TransientChannelException(
                    "[" + channel + "] Simulated transient failure (connection timeout)");
        }
    }

    private void simulateNetworkLatency(int minMs, int maxMs) {
        try {
            Thread.sleep(minMs + random.nextInt(maxMs - minMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
