package com.notificationservice.repository;

import com.notificationservice.domain.TenantRateLimitConfig;
import com.notificationservice.domain.Channel;
import com.notificationservice.domain.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRateLimitConfigRepository extends JpaRepository<TenantRateLimitConfig, UUID> {

    List<TenantRateLimitConfig> findByTenantId(UUID tenantId);

    /** Most specific match: tenant + channel + priority */
    Optional<TenantRateLimitConfig> findByTenantIdAndChannelAndPriority(
            UUID tenantId, Channel channel, Priority priority);

    /** Channel-level match (applies to all priorities for that channel) */
    Optional<TenantRateLimitConfig> findByTenantIdAndChannelAndPriorityIsNull(
            UUID tenantId, Channel channel);

    /** Global tenant-level match (applies to all channels and priorities) */
    Optional<TenantRateLimitConfig> findByTenantIdAndChannelIsNullAndPriorityIsNull(UUID tenantId);

    void deleteByTenantId(UUID tenantId);
}
