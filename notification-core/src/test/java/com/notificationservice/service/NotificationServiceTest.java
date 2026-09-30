package com.notificationservice.service;

import com.notificationservice.domain.*;
import com.notificationservice.dto.request.SendNotificationRequest;
import com.notificationservice.dto.response.NotificationResponse;
import com.notificationservice.pipeline.NotificationContext;
import com.notificationservice.pipeline.NotificationPipeline;
import com.notificationservice.repository.DeliveryAttemptRepository;
import com.notificationservice.repository.NotificationRequestRepository;
import com.notificationservice.repository.UserRepository;
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
class NotificationServiceTest {

    @Mock private NotificationPipeline pipeline;
    @Mock private NotificationRequestRepository requestRepository;
    @Mock private DeliveryAttemptRepository deliveryAttemptRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private NotificationService notificationService;

    @Test
    void shouldReturnExistingNotificationWhenDuplicate() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID existingId = UUID.randomUUID();

        User recipient = new User();
        recipient.setId(recipientId);
        recipient.setEmail("test@test.com");

        SendNotificationRequest req = new SendNotificationRequest();
        req.setTemplateId(UUID.randomUUID());
        req.setRecipientId(recipientId);
        req.setChannel(Channel.EMAIL);
        req.setIdempotencyKey("idemp-123");

        NotificationRequest existingRequest = new NotificationRequest();
        existingRequest.setId(existingId);
        existingRequest.setStatus(NotificationStatus.DELIVERED);

        when(userRepository.findByIdAndTenantId(recipientId, tenantId)).thenReturn(Optional.of(recipient));
        doAnswer(invocation -> {
            NotificationContext ctx = invocation.getArgument(0);
            ctx.setDuplicate(true);
            ctx.setExistingNotificationId(existingId);
            return null;
        }).when(pipeline).execute(any(NotificationContext.class));
        when(requestRepository.findById(existingId)).thenReturn(Optional.of(existingRequest));

        NotificationResponse response = notificationService.send(tenantId, userId, req);

        assertTrue(response.isDuplicate());
        assertEquals(existingId, response.getId());
        assertEquals(NotificationStatus.DELIVERED, response.getStatus());
    }

    @Test
    void shouldReturnNewNotificationWhenNotDuplicate() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();

        User recipient = new User();
        recipient.setId(recipientId);
        recipient.setEmail("test@test.com");

        SendNotificationRequest req = new SendNotificationRequest();
        req.setTemplateId(UUID.randomUUID());
        req.setRecipientId(recipientId);
        req.setChannel(Channel.EMAIL);

        NotificationRequest savedRequest = new NotificationRequest();
        savedRequest.setId(newId);
        savedRequest.setStatus(NotificationStatus.QUEUED);

        when(userRepository.findByIdAndTenantId(recipientId, tenantId)).thenReturn(Optional.of(recipient));
        doAnswer(invocation -> {
            NotificationContext ctx = invocation.getArgument(0);
            ctx.setDuplicate(false);
            ctx.setSavedRequest(savedRequest);
            return null;
        }).when(pipeline).execute(any(NotificationContext.class));

        NotificationResponse response = notificationService.send(tenantId, userId, req);

        assertFalse(response.isDuplicate());
        assertEquals(newId, response.getId());
        assertEquals(NotificationStatus.QUEUED, response.getStatus());
    }

    @Test
    void shouldThrowWhenRecipientNotFound() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        SendNotificationRequest req = new SendNotificationRequest();
        req.setTemplateId(UUID.randomUUID());
        req.setRecipientId(recipientId);
        req.setChannel(Channel.EMAIL);

        when(userRepository.findByIdAndTenantId(recipientId, tenantId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> notificationService.send(tenantId, userId, req));
    }

    @Test
    void shouldThrowWhenPhoneNumberMissingForSms() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        User recipient = new User();
        recipient.setId(recipientId);
        recipient.setPhoneNumber(null);

        SendNotificationRequest req = new SendNotificationRequest();
        req.setTemplateId(UUID.randomUUID());
        req.setRecipientId(recipientId);
        req.setChannel(Channel.SMS);

        when(userRepository.findByIdAndTenantId(recipientId, tenantId)).thenReturn(Optional.of(recipient));

        assertThrows(IllegalArgumentException.class, () -> notificationService.send(tenantId, userId, req));
    }

    @Test
    void shouldThrowWhenDeviceTokenMissingForPush() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        User recipient = new User();
        recipient.setId(recipientId);
        recipient.setDeviceToken(null);

        SendNotificationRequest req = new SendNotificationRequest();
        req.setTemplateId(UUID.randomUUID());
        req.setRecipientId(recipientId);
        req.setChannel(Channel.PUSH);

        when(userRepository.findByIdAndTenantId(recipientId, tenantId)).thenReturn(Optional.of(recipient));

        assertThrows(IllegalArgumentException.class, () -> notificationService.send(tenantId, userId, req));
    }
}
