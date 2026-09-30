package com.notificationservice;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.notificationservice.domain.*;
import com.notificationservice.dto.request.SendNotificationRequest;
import com.notificationservice.dto.response.NotificationResponse;
import com.notificationservice.repository.*;
import com.notificationservice.service.NotificationService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = NotificationServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class EmailDispatchIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig()
                    .withUser("noreply@acme.com", "smtp_user", "smtp_pass"))
            .withPerMethodLifecycle(true);

    @Autowired private NotificationService notificationService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationTemplateRepository templateRepository;
    @Autowired private NotificationChannelConfigRepository channelConfigRepository;
    @Autowired private NotificationRequestRepository requestRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private DeliveryAttemptRepository deliveryAttemptRepository;

    private UUID tenantId;
    private UUID userId;
    private UUID recipientId;
    private UUID templateId;

    @BeforeEach
    void setup() throws InterruptedException {
        // Wait briefly for any background dispatch threads from the previous test to finish
        // before we truncate the tables they may still be writing to
        Thread.sleep(300);

        deliveryAttemptRepository.deleteAll();
        requestRepository.deleteAll();
        templateRepository.deleteAll();
        channelConfigRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        Tenant tenant = tenantRepository.save(
                Tenant.builder().name("Test Corp").slug("testcorp").status(TenantStatus.ACTIVE).build());
        tenantId = tenant.getId();

        User admin = new User();
        admin.setTenantId(tenantId);
        admin.setEmail("admin@testcorp.com");
        admin.setPasswordHash("hash");
        admin.setRole(UserRole.TENANT_ADMIN);
        userId = userRepository.save(admin).getId();

        User recipient = new User();
        recipient.setTenantId(tenantId);
        recipient.setEmail("john.doe@example.com");
        recipient.setPasswordHash("hash");
        recipient.setRole(UserRole.TENANT_ADMIN);
        recipientId = userRepository.save(recipient).getId();

        // Point SMTP config at GreenMail's dynamic port
        channelConfigRepository.save(NotificationChannelConfig.builder()
                .tenantId(tenantId)
                .channel(Channel.EMAIL)
                .enabled(true)
                .config(Map.of(
                        "smtpHost",     "localhost",
                        "smtpPort",     String.valueOf(greenMail.getSmtp().getPort()),
                        "smtpUsername", "smtp_user",
                        "smtpPassword", "smtp_pass",
                        "fromAddress",  "noreply@acme.com"
                ))
                .build());

        NotificationTemplate template = templateRepository.save(
                NotificationTemplate.builder()
                        .tenantId(tenantId).name("Welcome").channel(Channel.EMAIL)
                        .subject("Welcome {{name}}!")
                        .bodyTemplate("<h1>Hello {{name}}</h1><p>Your account is ready.</p>")
                        .status(TemplateStatus.ACTIVE).build());
        templateId = template.getId();
    }

    @Test
    void shouldDeliverEmailViaSmtp() throws Exception {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setTemplateId(templateId);
        req.setChannel(Channel.EMAIL);
        req.setRecipientId(recipientId);
        req.setVariables(Map.of("name", "John"));

        NotificationResponse response = notificationService.send(tenantId, userId, req);

        assertNotNull(response.getId());
        assertEquals(NotificationStatus.QUEUED, response.getStatus());

        // Wait for the background dispatcher to complete the SMTP send (max 5s)
        assertTrue(greenMail.waitForIncomingEmail(5000, 1),
                "GreenMail did not receive the email within 5 seconds");

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);

        MimeMessage mail = received[0];
        assertEquals("Welcome John!", mail.getSubject());
        assertEquals("john.doe@example.com", mail.getAllRecipients()[0].toString());

        // GreenMailUtil correctly handles both plain and multipart MIME bodies
        String body = GreenMailUtil.getBody(mail);
        assertTrue(body.contains("Hello John"), "Email body should contain rendered name");
    }

    @Test
    void shouldDeliverBulkEmailsViaSmtp() throws Exception {
        User recipient2 = new User();
        recipient2.setTenantId(tenantId);
        recipient2.setEmail("jane.doe@example.com");
        recipient2.setPasswordHash("hash");
        recipient2.setRole(UserRole.TENANT_ADMIN);
        UUID recipient2Id = userRepository.save(recipient2).getId();

        SendNotificationRequest req1 = new SendNotificationRequest();
        req1.setTemplateId(templateId);
        req1.setChannel(Channel.EMAIL);
        req1.setRecipientId(recipientId);
        req1.setVariables(Map.of("name", "John"));
        notificationService.send(tenantId, userId, req1);

        SendNotificationRequest req2 = new SendNotificationRequest();
        req2.setTemplateId(templateId);
        req2.setChannel(Channel.EMAIL);
        req2.setRecipientId(recipient2Id);
        req2.setVariables(Map.of("name", "Jane"));
        notificationService.send(tenantId, userId, req2);

        assertTrue(greenMail.waitForIncomingEmail(5000, 2),
                "GreenMail should receive 2 emails within 5 seconds");

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(2, received.length);

        boolean johnReceived = false, janeReceived = false;
        for (MimeMessage m : received) {
            String to = m.getAllRecipients()[0].toString();
            if (to.equals("john.doe@example.com")) { assertEquals("Welcome John!", m.getSubject()); johnReceived = true; }
            else if (to.equals("jane.doe@example.com")) { assertEquals("Welcome Jane!", m.getSubject()); janeReceived = true; }
        }
        assertTrue(johnReceived, "John should have received an email");
        assertTrue(janeReceived, "Jane should have received an email");
    }

    @Test
    void shouldNotSendEmailWhenChannelDisabled() {
        channelConfigRepository.findAll().forEach(cfg -> {
            cfg.setEnabled(false);
            channelConfigRepository.save(cfg);
        });

        SendNotificationRequest req = new SendNotificationRequest();
        req.setTemplateId(templateId);
        req.setChannel(Channel.EMAIL);
        req.setRecipientId(recipientId);
        req.setVariables(Map.of("name", "John"));

        assertThrows(Exception.class, () -> notificationService.send(tenantId, userId, req));
        assertEquals(0, greenMail.getReceivedMessages().length);
    }
}
