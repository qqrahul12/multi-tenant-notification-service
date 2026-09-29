package com.notificationservice;

import com.notificationservice.domain.*;
import com.notificationservice.dto.request.SendNotificationRequest;
import com.notificationservice.dto.response.NotificationResponse;
import com.notificationservice.repository.NotificationRequestRepository;
import com.notificationservice.repository.NotificationTemplateRepository;
import com.notificationservice.repository.NotificationChannelConfigRepository;
import com.notificationservice.repository.TenantRepository;
import com.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@org.junit.jupiter.api.Disabled("Requires Docker running (Testcontainers)")
class NotificationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired private NotificationService notificationService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private NotificationTemplateRepository templateRepository;
    @Autowired private NotificationChannelConfigRepository channelConfigRepository;
    @Autowired private NotificationRequestRepository requestRepository;

    private UUID tenantId;
    private UUID templateId;
    private UUID userId;

    @BeforeEach
    void setup() {
        requestRepository.deleteAll();
        templateRepository.deleteAll();
        channelConfigRepository.deleteAll();
        tenantRepository.deleteAll();

        userId = UUID.randomUUID();

        // 1. Create Tenant
        Tenant tenant = Tenant.builder()
                .name("Acme Corp")
                .slug("acme")
                .status(TenantStatus.ACTIVE)
                .build();
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();

        // 2. Create Channel Config
        NotificationChannelConfig channelConfig = NotificationChannelConfig.builder()
                .tenantId(tenantId)
                .channel(Channel.EMAIL)
                .enabled(true)
                .build();
        channelConfigRepository.save(channelConfig);

        // 3. Create Template
        NotificationTemplate template = NotificationTemplate.builder()
                .tenantId(tenantId)
                .name("Welcome Email")
                .channel(Channel.EMAIL)
                .subject("Welcome {{name}}!")
                .bodyTemplate("Hello {{name}}, welcome to our platform.")
                .status(TemplateStatus.ACTIVE)
                .build();
        template = templateRepository.save(template);
        templateId = template.getId();
    }

    @Test
    void shouldSuccessfullyProcessNotificationThroughPipeline() {
        // Arrange
        SendNotificationRequest request = new SendNotificationRequest();
        request.setTemplateId(templateId);
        request.setChannel(Channel.EMAIL);
        request.setRecipientAddress("user@example.com");
        request.setVariables(Map.of("name", "John"));

        // Act
        NotificationResponse response = notificationService.send(tenantId, userId, request);

        // Assert
        assertNotNull(response.getId());
        assertEquals(NotificationStatus.QUEUED, response.getStatus());
        
        NotificationRequest saved = requestRepository.findById(response.getId()).orElseThrow();
        assertEquals("Welcome John!", saved.getRenderedSubject());
        assertEquals("Hello John, welcome to our platform.", saved.getRenderedBody());
        assertEquals("user@example.com", saved.getRecipientAddress());
    }

    @Test
    void shouldDeduplicateRequestsWithSameIdempotencyKey() {
        // Arrange
        String idempotencyKey = "idemp-test-123";

        SendNotificationRequest req1 = new SendNotificationRequest();
        req1.setTemplateId(templateId);
        req1.setChannel(Channel.EMAIL);
        req1.setRecipientAddress("user1@example.com");
        req1.setVariables(Map.of("name", "Alice"));
        req1.setIdempotencyKey(idempotencyKey);

        SendNotificationRequest req2 = new SendNotificationRequest();
        req2.setTemplateId(templateId);
        req2.setChannel(Channel.EMAIL);
        req2.setRecipientAddress("user2@example.com"); // Different data, but same key
        req2.setVariables(Map.of("name", "Bob"));
        req2.setIdempotencyKey(idempotencyKey);

        // Act
        NotificationResponse response1 = notificationService.send(tenantId, userId, req1);
        NotificationResponse response2 = notificationService.send(tenantId, userId, req2);

        // Assert
        assertFalse(response1.isDuplicate());
        assertTrue(response2.isDuplicate());
        assertEquals(response1.getId(), response2.getId()); // Second request returns first request's ID

        long count = requestRepository.count();
        assertEquals(1, count); // Only 1 record in DB
    }
}
