package com.notificationservice.scheduler;

import com.notificationservice.channel.ChannelDispatcherFactory;
import com.notificationservice.channel.ChannelDispatcher;
import com.notificationservice.channel.RetryAwareChannelDispatcher;
import com.notificationservice.domain.Channel;
import com.notificationservice.domain.NotificationRequest;
import com.notificationservice.repository.NotificationRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.concurrent.ExecutorService;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock private NotificationRequestRepository requestRepository;
    @Mock private ChannelDispatcherFactory dispatcherFactory;
    @Mock private ExecutorService executorService;
    @Mock private RetryAwareChannelDispatcher retryAwareDispatcher;
    @Mock private ChannelDispatcher channelDispatcher;

    private NotificationScheduler scheduler;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        scheduler = new NotificationScheduler(requestRepository, dispatcherFactory, retryAwareDispatcher, executorService, 100);
    }

    @Test
    void shouldDispatchPendingNotifications() {
        NotificationRequest request1 = new NotificationRequest();
        request1.setId(java.util.UUID.randomUUID());
        request1.setChannel(Channel.EMAIL);

        NotificationRequest request2 = new NotificationRequest();
        request2.setId(java.util.UUID.randomUUID());
        request2.setChannel(Channel.SMS);

        when(requestRepository.findPendingForDispatch(any(), anyInt())).thenReturn(List.of(request1, request2));
        when(dispatcherFactory.getDispatcher(any())).thenReturn(channelDispatcher);

        scheduler.dispatchScheduledNotifications();

        verify(executorService, times(2)).submit(any(Runnable.class));
    }
}
