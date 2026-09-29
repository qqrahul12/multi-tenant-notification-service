package com.notificationservice.service;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.NotificationChannelConfig;
import com.notificationservice.dto.request.UpsertChannelConfigRequest;
import com.notificationservice.repository.NotificationChannelConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelConfigServiceTest {

    @Mock private NotificationChannelConfigRepository configRepository;
    @InjectMocks private ChannelConfigService channelConfigService;

    @Test
    void shouldUpsertNewConfig() {
        UUID tenantId = UUID.randomUUID();
        UpsertChannelConfigRequest req = new UpsertChannelConfigRequest();
        req.setChannel(Channel.SMS);
        req.setEnabled(true);
        req.setConfig(java.util.Map.of("apiKey", "123"));

        when(configRepository.findByTenantIdAndChannel(tenantId, Channel.SMS)).thenReturn(Optional.empty());
        when(configRepository.save(any())).thenAnswer(i -> {
            NotificationChannelConfig c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        NotificationChannelConfig result = channelConfigService.upsert(tenantId, req);
        assertNotNull(result.getId());
        assertEquals("123", result.getConfig().get("apiKey"));
    }

    @Test
    void shouldGetConfig() {
        UUID tenantId = UUID.randomUUID();
        NotificationChannelConfig config = new NotificationChannelConfig();
        config.setChannel(Channel.EMAIL);
        when(configRepository.findByTenantIdAndChannel(tenantId, Channel.EMAIL)).thenReturn(Optional.of(config));

        NotificationChannelConfig result = channelConfigService.getConfig(tenantId, Channel.EMAIL);
        assertNotNull(result);
        assertEquals(Channel.EMAIL, result.getChannel());
    }
}
