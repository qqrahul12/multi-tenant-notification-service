package com.notificationservice;

import com.notificationservice.domain.*;
import com.notificationservice.dto.request.SendNotificationRequest;
import com.notificationservice.dto.response.NotificationResponse;
import com.notificationservice.repository.*;
import com.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = NotificationServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class NotificationIntegrationTest {

    @Autowired private NotificationService notificationService;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private NotificationTemplateRepository templateRepository;
    @Autowired private NotificationChannelConfigRepository channelConfigRepository;
    @Autowired private NotificationRequestRepository requestRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRateLimitConfigRepository rateLimitConfigRepository;

    private UUID tenantId;
    private UUID templateId;
    private UUID userId;     // the acting TENANT_ADMIN
    private UUID recipientId; // the end-user who receives notifications

    @BeforeEach
    void setup() {
        requestRepository.deleteAll();
        templateRepository.deleteAll();
        channelConfigRepository.deleteAll();
        rateLimitConfigRepository.deleteAll();
        userRepository.deleteAll();
        auditLogRepository.deleteAll();
        tenantRepository.deleteAll();

        // 1. Create Tenant
        Tenant tenant = Tenant.builder().name("Acme Corp").slug("acme").status(TenantStatus.ACTIVE).build();
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();

        // 2. Create acting admin user
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setTenantId(tenantId);
        admin.setEmail("admin@acme.com");
        admin.setPasswordHash("hash");
        admin.setRole(UserRole.TENANT_ADMIN);
        admin = userRepository.save(admin);
        userId = admin.getId();

        // 3. Create end recipient user with all contact fields
        User recipient = new User();
        recipient.setId(UUID.randomUUID());
        recipient.setTenantId(tenantId);
        recipient.setEmail("user@example.com");
        recipient.setPhoneNumber("+1234567890");
        recipient.setDeviceToken("fcm_device_token_abc123");
        recipient.setPasswordHash("hash");
        recipient.setRole(UserRole.TENANT_ADMIN);
        recipient = userRepository.save(recipient);
        recipientId = recipient.getId();

        // 4. Create EMAIL channel config
        channelConfigRepository.save(NotificationChannelConfig.builder()
                .tenantId(tenantId).channel(Channel.EMAIL).enabled(true)
                .config(Map.of("smtpHost", "smtp.mailtrap.io", "smtpPort", "587",
                               "smtpUsername", "mock_user", "smtpPassword", "mock_pass",
                               "fromAddress", "noreply@acme.com"))
                .build());

        // 5. Create EMAIL template
        NotificationTemplate template = NotificationTemplate.builder()
                .tenantId(tenantId).name("Welcome Email").channel(Channel.EMAIL)
                .subject("Welcome {{name}}!")
                .bodyTemplate("Hello {{name}}, welcome to our platform.")
                .status(TemplateStatus.ACTIVE).build();
        template = templateRepository.save(template);
        templateId = template.getId();
    }

    @Test
    void shouldSuccessfullyProcessNotificationThroughPipeline() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setTemplateId(templateId);
        request.setChannel(Channel.EMAIL);
        request.setRecipientId(recipientId);
        request.setVariables(Map.of("name", "John"));

        NotificationResponse response = notificationService.send(tenantId, userId, request);

        assertNotNull(response.getId());
        assertEquals(NotificationStatus.QUEUED, response.getStatus());

        NotificationRequest saved = requestRepository.findById(response.getId()).orElseThrow();
        assertEquals("Welcome John!", saved.getRenderedSubject());
        assertEquals("Hello John, welcome to our platform.", saved.getRenderedBody());
        assertEquals("user@example.com", saved.getRecipientAddress());
    }

    @Test
    void shouldDeduplicateRequestsWithSameIdempotencyKey() {
        String idempotencyKey = "idemp-test-123";

        SendNotificationRequest req1 = new SendNotificationRequest();
        req1.setTemplateId(templateId);
        req1.setChannel(Channel.EMAIL);
        req1.setRecipientId(recipientId);
        req1.setVariables(Map.of("name", "Alice"));
        req1.setIdempotencyKey(idempotencyKey);

        SendNotificationRequest req2 = new SendNotificationRequest();
        req2.setTemplateId(templateId);
        req2.setChannel(Channel.EMAIL);
        req2.setRecipientId(recipientId);
        req2.setVariables(Map.of("name", "Bob"));
        req2.setIdempotencyKey(idempotencyKey);

        NotificationResponse response1 = notificationService.send(tenantId, userId, req1);
        NotificationResponse response2 = notificationService.send(tenantId, userId, req2);

        assertFalse(response1.isDuplicate());
        assertTrue(response2.isDuplicate());
        assertEquals(response1.getId(), response2.getId());
        assertEquals(1, requestRepository.count());
    }

    @Test
    void shouldSuccessfullyProcessSmsAndPush() {
        channelConfigRepository.save(NotificationChannelConfig.builder()
                .tenantId(tenantId).channel(Channel.SMS).enabled(true)
                .config(Map.of("gupshupApiUrl", "https://enterprise.smsgupshup.com/GatewayAPI/rest",
                               "gupshupUserId", "mock_user", "gupshupPassword", "mock_pass"))
                .build());

        channelConfigRepository.save(NotificationChannelConfig.builder()
                .tenantId(tenantId).channel(Channel.PUSH).enabled(true)
                .config(Map.of("fcmApiUrl", "https://fcm.googleapis.com/fcm/send",
                               "fcmServerKey", "mock_key"))
                .build());

        NotificationTemplate smsTemplate = templateRepository.save(NotificationTemplate.builder()
                .tenantId(tenantId).name("OTP SMS").channel(Channel.SMS)
                .bodyTemplate("Your OTP is {{code}}")
                .status(TemplateStatus.ACTIVE).build());

        SendNotificationRequest req = new SendNotificationRequest();
        req.setTemplateId(smsTemplate.getId());
        req.setChannel(Channel.SMS);
        req.setRecipientId(recipientId);
        req.setVariables(Map.of("code", "999"));

        NotificationResponse resp = notificationService.send(tenantId, userId, req);
        NotificationRequest saved = requestRepository.findById(resp.getId()).orElseThrow();
        assertEquals("Your OTP is 999", saved.getRenderedBody());
        assertEquals("+1234567890", saved.getRecipientAddress());
        assertEquals(Channel.SMS, saved.getChannel());
    }

    @Test
    void shouldEnforceRateLimiting() {
        rateLimitConfigRepository.save(TenantRateLimitConfig.builder()
                .tenantId(tenantId).limitPerMinute(1).burstCapacity(1).build());

        SendNotificationRequest req1 = new SendNotificationRequest();
        req1.setTemplateId(templateId);
        req1.setChannel(Channel.EMAIL);
        req1.setRecipientId(recipientId);
        req1.setVariables(Map.of("name", "Test"));

        SendNotificationRequest req2 = new SendNotificationRequest();
        req2.setTemplateId(templateId);
        req2.setChannel(Channel.EMAIL);
        req2.setRecipientId(recipientId);
        req2.setVariables(Map.of("name", "Test"));

        NotificationResponse resp1 = notificationService.send(tenantId, userId, req1);
        assertEquals(NotificationStatus.QUEUED, resp1.getStatus());

        assertThrows(com.notificationservice.exception.RateLimitExceededException.class,
                () -> notificationService.send(tenantId, userId, req2));
    }

    @Test
    void shouldQueueScheduledNotifications() {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setTemplateId(templateId);
        req.setChannel(Channel.EMAIL);
        req.setRecipientId(recipientId);
        req.setVariables(Map.of("name", "Future"));
        req.setScheduledAt(java.time.Instant.now().plusSeconds(3600).toString());

        NotificationResponse resp = notificationService.send(tenantId, userId, req);
        assertEquals(NotificationStatus.PENDING, resp.getStatus());

        NotificationRequest saved = requestRepository.findById(resp.getId()).orElseThrow();
        assertNotNull(saved.getScheduledAt());
    }
}
