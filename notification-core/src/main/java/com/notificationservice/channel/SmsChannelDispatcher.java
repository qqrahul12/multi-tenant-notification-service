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
public class SmsChannelDispatcher extends AbstractChannelDispatcher {

    private final RestTemplate restTemplate;

    @Override
    protected String doSend(NotificationRequest request, NotificationChannelConfig config) {
        Map<String, String> cfg = config.getConfig();
        
        if (!cfg.containsKey("gupshupApiUrl") || !cfg.containsKey("gupshupUserId") || !cfg.containsKey("gupshupPassword")) {
            throw new IllegalArgumentException("Missing required Gupshup configuration (gupshupApiUrl, gupshupUserId, gupshupPassword)");
        }

        String apiUrl = cfg.get("gupshupApiUrl");
        String userId = cfg.get("gupshupUserId");
        String password = cfg.get("gupshupPassword");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Gupshup standard Enterprise SMS API parameters
        String payload = String.format("method=SendMessage&send_to=%s&msg=%s&msg_type=TEXT&userid=%s&auth_scheme=plain&password=%s&v=1.1&format=json",
                request.getRecipientAddress(),
                request.getRenderedBody(),
                userId,
                password);

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new TransientChannelException("Gupshup API rejected request with status: " + response.getStatusCode());
        }

        return response.getBody();
    }

    @Override
    public Channel getSupportedChannel() { return Channel.SMS; }
}
