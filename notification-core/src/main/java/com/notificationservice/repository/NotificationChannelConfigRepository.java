package com.notificationservice.repository;

import com.notificationservice.domain.NotificationChannelConfig;
import com.notificationservice.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationChannelConfigRepository extends JpaRepository<NotificationChannelConfig, UUID> {

    List<NotificationChannelConfig> findByTenantId(UUID tenantId);

    Optional<NotificationChannelConfig> findByTenantIdAndChannel(UUID tenantId, Channel channel);

    boolean existsByTenantIdAndChannel(UUID tenantId, Channel channel);

    List<NotificationChannelConfig> findByTenantIdAndEnabled(UUID tenantId, boolean enabled);
}
