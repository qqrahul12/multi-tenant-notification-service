package com.notificationservice.service;

import com.notificationservice.domain.NotificationTemplate;
import com.notificationservice.domain.TemplateStatus;
import com.notificationservice.dto.request.CreateTemplateRequest;
import com.notificationservice.exception.TemplateNotFoundException;
import com.notificationservice.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final NotificationTemplateRepository templateRepository;

    @Transactional
    public NotificationTemplate createTemplate(UUID tenantId, CreateTemplateRequest req) {
        if (templateRepository.existsByTenantIdAndNameAndChannel(tenantId, req.getName(), req.getChannel())) {
            throw new IllegalArgumentException(
                    "Template with name '" + req.getName() + "' and channel '" + req.getChannel() +
                    "' already exists for this tenant.");
        }
        NotificationTemplate template = NotificationTemplate.builder()
                .tenantId(tenantId)
                .name(req.getName())
                .channel(req.getChannel())
                .subject(req.getSubject())
                .bodyTemplate(req.getBodyTemplate())
                .build();
        return templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public Page<NotificationTemplate> listTemplates(UUID tenantId, TemplateStatus status, Pageable pageable) {
        return status != null
                ? templateRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : templateRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "templates", key = "#tenantId + ':' + #templateId")
    public NotificationTemplate getTemplate(UUID tenantId, UUID templateId) {
        return templateRepository.findByIdAndTenantId(templateId, tenantId)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found: " + templateId));
    }

    @Transactional
    @CacheEvict(value = "templates", key = "#tenantId + ':' + #templateId")
    public NotificationTemplate updateTemplate(UUID tenantId, UUID templateId, CreateTemplateRequest req) {
        NotificationTemplate template = getTemplate(tenantId, templateId);
        template.setName(req.getName());
        template.setSubject(req.getSubject());
        template.setBodyTemplate(req.getBodyTemplate());
        return templateRepository.save(template);
    }

    @Transactional
    @CacheEvict(value = "templates", key = "#tenantId + ':' + #templateId")
    public void deleteTemplate(UUID tenantId, UUID templateId) {
        NotificationTemplate template = getTemplate(tenantId, templateId);
        template.setStatus(TemplateStatus.INACTIVE);  // soft delete
        templateRepository.save(template);
    }
}
