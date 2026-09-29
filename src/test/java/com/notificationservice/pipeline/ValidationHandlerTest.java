package com.notificationservice.pipeline;

import com.notificationservice.domain.*;
import com.notificationservice.exception.TemplateNotFoundException;
import com.notificationservice.exception.TenantNotFoundException;
import com.notificationservice.repository.NotificationChannelConfigRepository;
import com.notificationservice.repository.NotificationTemplateRepository;
import com.notificationservice.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
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
class ValidationHandlerTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private NotificationTemplateRepository templateRepository;
    @Mock private NotificationChannelConfigRepository channelConfigRepository;
    @Mock private NotificationHandlerChain chain;

    @InjectMocks private ValidationHandler validationHandler;

    private NotificationContext context;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID templateId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        context = NotificationContext.builder()
                .tenantId(tenantId)
                .templateId(templateId)
                .channel(Channel.EMAIL)
                .build();
    }

    @Test
    void shouldPassValidationWhenEverythingIsValid() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        
        NotificationTemplate template = new NotificationTemplate();
        template.setId(templateId);
        template.setChannel(Channel.EMAIL);
        template.setStatus(TemplateStatus.ACTIVE);

        NotificationChannelConfig config = new NotificationChannelConfig();
        config.setEnabled(true);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(templateRepository.findByIdAndTenantId(templateId, tenantId)).thenReturn(Optional.of(template));
        when(channelConfigRepository.findByTenantIdAndChannel(tenantId, Channel.EMAIL)).thenReturn(Optional.of(config));

        validationHandler.handle(context, chain);

        verify(chain).next(context);
        assertEquals(tenant, context.getTenant());
        assertEquals(template, context.getTemplate());
        assertEquals(config, context.getChannelConfig());
    }

    @Test
    void shouldThrowWhenTenantNotFound() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
        assertThrows(TenantNotFoundException.class, () -> validationHandler.handle(context, chain));
        verifyNoInteractions(chain);
    }

    @Test
    void shouldThrowWhenTemplateMismatchesChannel() {
        Tenant tenant = new Tenant();
        NotificationTemplate template = new NotificationTemplate();
        template.setChannel(Channel.SMS); // Mismatch
        template.setStatus(TemplateStatus.ACTIVE);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(templateRepository.findByIdAndTenantId(templateId, tenantId)).thenReturn(Optional.of(template));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
            () -> validationHandler.handle(context, chain));
        assertTrue(ex.getMessage().contains("Template channel mismatch"));
    }
}
