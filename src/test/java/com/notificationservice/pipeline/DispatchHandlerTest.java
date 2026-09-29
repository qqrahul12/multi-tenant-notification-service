package com.notificationservice.pipeline;

import com.notificationservice.channel.ChannelDispatcherFactory;
import com.notificationservice.channel.ChannelDispatcher;
import com.notificationservice.channel.RetryAwareChannelDispatcher;
import com.notificationservice.domain.Channel;
import com.notificationservice.domain.NotificationRequest;
import com.notificationservice.domain.NotificationStatus;
import com.notificationservice.repository.NotificationRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchHandlerTest {

    @Mock private NotificationRequestRepository requestRepository;
    @Mock private ChannelDispatcherFactory dispatcherFactory;
    @Mock private ExecutorService executorService;
    @Mock private RetryAwareChannelDispatcher retryAwareDispatcher;
    @Mock private ChannelDispatcher channelDispatcher;
    @Mock private NotificationHandlerChain chain;

    @InjectMocks private DispatchHandler dispatchHandler;

    @Test
    void shouldPersistAndSubmitForImmediateDispatch() {
        NotificationContext context = NotificationContext.builder()
                .tenantId(UUID.randomUUID())
                .channel(Channel.EMAIL)
                .build();

        NotificationRequest savedRequest = new NotificationRequest();
        savedRequest.setId(UUID.randomUUID());
        savedRequest.setChannel(Channel.EMAIL);

        // First save for persistence, second save after transition to QUEUED
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(savedRequest);
        when(dispatcherFactory.getDispatcher(Channel.EMAIL)).thenReturn(channelDispatcher);

        dispatchHandler.handle(context, chain);

        verify(executorService).submit(any(Runnable.class));
        verify(requestRepository, times(2)).save(any(NotificationRequest.class));
        assertEquals(NotificationStatus.QUEUED, savedRequest.getStatus());
        verify(chain).next(context);
    }

    @Test
    void shouldPersistAndLeavePendingForScheduledDispatch() {
        NotificationContext context = NotificationContext.builder()
                .tenantId(UUID.randomUUID())
                .channel(Channel.EMAIL)
                .scheduledAt("2099-12-31T23:59:59Z") // Scheduled
                .build();

        NotificationRequest savedRequest = new NotificationRequest();
        savedRequest.setId(UUID.randomUUID());
        savedRequest.setChannel(Channel.EMAIL);
        savedRequest.setStatus(NotificationStatus.PENDING); // Left as pending
        savedRequest.setScheduledAt(java.time.Instant.parse("2099-12-31T23:59:59Z"));

        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(savedRequest);

        dispatchHandler.handle(context, chain);

        // Should not submit to executor
        verifyNoInteractions(executorService);
        verify(requestRepository, times(1)).save(any(NotificationRequest.class));
        verify(chain).next(context);
    }
}
