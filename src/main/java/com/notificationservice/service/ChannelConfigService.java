package com.notificationservice.service;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.NotificationChannelConfig;
import com.notificationservice.dto.request.UpsertChannelConfigRequest;
import com.notificationservice.repository.NotificationChannelConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChannelConfigService {

    private final NotificationChannelConfigRepository configRepository;

    @Transactional
    @CacheEvict(value = "channelConfigs", key = "#tenantId")
    public NotificationChannelConfig upsert(UUID tenantId, UpsertChannelConfigRequest req) {
        NotificationChannelConfig config = configRepository
                .findByTenantIdAndChannel(tenantId, req.getChannel())
                .orElseGet(() -> NotificationChannelConfig.builder()
                        .tenantId(tenantId)
                        .channel(req.getChannel())
                        .build());

        if (req.getConfig() != null) {
            config.setConfig(req.getConfig());
        }
        config.setEnabled(req.isEnabled());
        return configRepository.save(config);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "channelConfigs", key = "#tenantId")
    public List<NotificationChannelConfig> listConfigs(UUID tenantId) {
        return configRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public NotificationChannelConfig getConfig(UUID tenantId, Channel channel) {
        return configRepository.findByTenantIdAndChannel(tenantId, channel)
                .orElseThrow(() -> new IllegalStateException(
                        "No config found for channel: " + channel));
    }
}
