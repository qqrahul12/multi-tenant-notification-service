package com.notificationservice.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationservice.domain.Channel;
import com.notificationservice.domain.NotificationChannelConfig;
import com.notificationservice.domain.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class InAppChannelDispatcher extends AbstractChannelDispatcher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected String doSend(NotificationRequest request, NotificationChannelConfig config) throws Exception {
        Map<String, String> cfg = config.getConfig();
        String redisTopic = cfg.getOrDefault("redisTopic", "in_app_notifications");

        // Broadcast to WebSocket servers via Redis PubSub
        Map<String, Object> payload = Map.of(
            "notificationId", request.getId().toString(),
            "tenantId", request.getTenantId().toString(),
            "recipient", request.getRecipientAddress(),
            "subject", request.getRenderedSubject(),
            "body", request.getRenderedBody()
        );

        String jsonPayload = objectMapper.writeValueAsString(payload);
        redisTemplate.convertAndSend(redisTopic, jsonPayload);

        return "REDIS_PUBSUB_" + request.getId();
    }

    @Override
    public Channel getSupportedChannel() { return Channel.IN_APP; }
}
