package com.notificationservice.service;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.NotificationTemplate;
import com.notificationservice.dto.request.CreateTemplateRequest;
import com.notificationservice.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock private NotificationTemplateRepository templateRepository;
    @InjectMocks private TemplateService templateService;

    @Test
    void shouldCreateTemplate() {
        UUID tenantId = UUID.randomUUID();
        CreateTemplateRequest req = new CreateTemplateRequest();
        req.setName("Welcome");
        req.setChannel(Channel.EMAIL);
        req.setSubject("Hello");
        req.setBodyTemplate("World");

        when(templateRepository.existsByTenantIdAndNameAndChannel(tenantId, "Welcome", Channel.EMAIL))
                .thenReturn(false);
        
        when(templateRepository.save(any())).thenAnswer(i -> {
            NotificationTemplate t = i.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        NotificationTemplate result = templateService.createTemplate(tenantId, req);
        assertNotNull(result.getId());
        assertEquals("Welcome", result.getName());
    }
}
