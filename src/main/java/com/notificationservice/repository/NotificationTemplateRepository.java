package com.notificationservice.repository;

import com.notificationservice.domain.NotificationTemplate;
import com.notificationservice.domain.Channel;
import com.notificationservice.domain.TemplateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Page<NotificationTemplate> findByTenantId(UUID tenantId, Pageable pageable);

    Page<NotificationTemplate> findByTenantIdAndStatus(UUID tenantId, TemplateStatus status, Pageable pageable);

    Optional<NotificationTemplate> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndNameAndChannel(UUID tenantId, String name, Channel channel);
}
