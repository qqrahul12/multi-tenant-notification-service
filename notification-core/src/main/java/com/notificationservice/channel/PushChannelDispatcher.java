package com.notificationservice.channel;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.NotificationChannelConfig;
import com.notificationservice.domain.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class PushChannelDispatcher extends AbstractChannelDispatcher {

    private final RestTemplate restTemplate;

    @Override
    protected String doSend(NotificationRequest request, NotificationChannelConfig config) {
        Map<String, String> cfg = config.getConfig();
        
        if (!cfg.containsKey("fcmApiUrl") || !cfg.containsKey("fcmServerKey")) {
            throw new IllegalArgumentException("Missing required FCM configuration (fcmApiUrl, fcmServerKey)");
        }

        String apiUrl = cfg.get("fcmApiUrl");
        String serverKey = cfg.get("fcmServerKey");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(serverKey);

        Map<String, Object> payload = Map.of(
            "to", request.getRecipientAddress(),
            "notification", Map.of(
                "title", request.getRenderedSubject(),
                "body", request.getRenderedBody()
            )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new TransientChannelException("FCM API rejected request with status: " + response.getStatusCode());
        }

        return response.getBody();
    }

    @Override
    public Channel getSupportedChannel() { return Channel.PUSH; }
}
