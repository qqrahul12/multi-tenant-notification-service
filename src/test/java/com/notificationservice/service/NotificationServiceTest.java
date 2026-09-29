package com.notificationservice.service;

import com.notificationservice.domain.*;
import com.notificationservice.dto.request.SendNotificationRequest;
import com.notificationservice.dto.response.NotificationResponse;
import com.notificationservice.pipeline.NotificationContext;
import com.notificationservice.pipeline.NotificationPipeline;
import com.notificationservice.repository.NotificationRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationPipeline pipeline;

    @Mock
    private NotificationRequestRepository requestRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void shouldReturnExistingNotificationWhenDuplicate() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID existingId = UUID.randomUUID();
        
        SendNotificationRequest req = new SendNotificationRequest();
        req.setTemplateId(UUID.randomUUID());
        req.setRecipientAddress("test@test.com");
        req.setChannel(Channel.EMAIL);
        req.setIdempotencyKey("idemp-123");

        NotificationRequest existingRequest = new NotificationRequest();
        existingRequest.setId(existingId);
        existingRequest.setStatus(NotificationStatus.DELIVERED);

        doAnswer(invocation -> {
            NotificationContext ctx = invocation.getArgument(0);
            ctx.setDuplicate(true);
            ctx.setExistingNotificationId(existingId);
            return null;
        }).when(pipeline).execute(any(NotificationContext.class));

        when(requestRepository.findById(existingId)).thenReturn(Optional.of(existingRequest));

        // Act
        NotificationResponse response = notificationService.send(tenantId, userId, req);

        // Assert
        assertTrue(response.isDuplicate());
        assertEquals(existingId, response.getId());
        assertEquals(NotificationStatus.DELIVERED, response.getStatus());
    }

    @Test
    void shouldReturnNewNotificationWhenNotDuplicate() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();

        SendNotificationRequest req = new SendNotificationRequest();
        req.setTemplateId(UUID.randomUUID());
        req.setRecipientAddress("test@test.com");
        req.setChannel(Channel.EMAIL);

        NotificationRequest savedRequest = new NotificationRequest();
        savedRequest.setId(newId);
        savedRequest.setStatus(NotificationStatus.QUEUED);

        doAnswer(invocation -> {
            NotificationContext ctx = invocation.getArgument(0);
            ctx.setDuplicate(false);
            ctx.setSavedRequest(savedRequest);
            return null;
        }).when(pipeline).execute(any(NotificationContext.class));

        // Act
        NotificationResponse response = notificationService.send(tenantId, userId, req);

        // Assert
        assertFalse(response.isDuplicate());
        assertEquals(newId, response.getId());
        assertEquals(NotificationStatus.QUEUED, response.getStatus());
    }
}
