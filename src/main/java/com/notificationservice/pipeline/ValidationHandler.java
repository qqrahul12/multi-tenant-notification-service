package com.notificationservice.pipeline;

import com.notificationservice.domain.NotificationChannelConfig;
import com.notificationservice.domain.NotificationTemplate;
import com.notificationservice.domain.Tenant;
import com.notificationservice.exception.TemplateNotFoundException;
import com.notificationservice.exception.TenantNotFoundException;
import com.notificationservice.repository.NotificationChannelConfigRepository;
import com.notificationservice.repository.NotificationTemplateRepository;
import com.notificationservice.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Handler 1 — Validates the request:
 * - Tenant exists
 * - Template exists and belongs to the tenant
 * - Channel config exists and is enabled
 * - Template channel matches request channel
 */
@Component
@RequiredArgsConstructor
public class ValidationHandler implements NotificationHandler {

    private final TenantRepository tenantRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationChannelConfigRepository channelConfigRepository;

    @Override
    public void handle(NotificationContext context, NotificationHandlerChain chain) {
        // Resolve tenant
        Tenant tenant = tenantRepository.findById(context.getTenantId())
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + context.getTenantId()));
        context.setTenant(tenant);

        // Resolve template
        NotificationTemplate template = templateRepository
                .findByIdAndTenantId(context.getTemplateId(), context.getTenantId())
                .orElseThrow(() -> new TemplateNotFoundException(
                        "Template not found: " + context.getTemplateId()));

        if (!template.isActive()) {
            throw new IllegalStateException("Template is inactive: " + template.getId());
        }

        if (template.getChannel() != context.getChannel()) {
            throw new IllegalArgumentException(
                    "Template channel mismatch. Template: " + template.getChannel() +
                    ", Request: " + context.getChannel());
        }
        context.setTemplate(template);

        // Resolve channel config
        NotificationChannelConfig channelConfig = channelConfigRepository
                .findByTenantIdAndChannel(context.getTenantId(), context.getChannel())
                .orElseThrow(() -> new IllegalStateException(
                        "No channel config for: " + context.getChannel() +
                        ". Please configure the channel first."));

        if (!channelConfig.isEnabled()) {
            throw new IllegalStateException("Channel is disabled: " + context.getChannel());
        }
        context.setChannelConfig(channelConfig);

        chain.next(context);
    }

    @Override
    public int getOrder() { return 1; }
}
