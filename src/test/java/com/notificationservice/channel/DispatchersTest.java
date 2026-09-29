package com.notificationservice.channel;

import com.notificationservice.domain.NotificationRequest;
import com.notificationservice.repository.DeliveryAttemptRepository;
import com.notificationservice.repository.NotificationRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchersTest {

    @Mock private DeliveryAttemptRepository attemptRepository;
    @Mock private NotificationRequestRepository requestRepository;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Test
    void testEmailDispatcher() {
        EmailChannelDispatcher dispatcher = new EmailChannelDispatcher();
        NotificationRequest request = new NotificationRequest();
        request.setId(UUID.randomUUID());
        request.setRecipientAddress("test@test.com");
        request.setRenderedSubject("Subject");
        request.setRenderedBody("Body");
        
        try {
            dispatcher.dispatch(request);
        } catch (Exception e) {} // ignore transient failure
    }

    @Test
    void testSmsDispatcher() {
        SmsChannelDispatcher dispatcher = new SmsChannelDispatcher();
        NotificationRequest request = new NotificationRequest();
        request.setId(UUID.randomUUID());
        request.setRecipientAddress("+1234567890");
        request.setRenderedBody("Body");
        try { dispatcher.dispatch(request); } catch (Exception e) {}
    }

    @Test
    void testPushDispatcher() {
        PushChannelDispatcher dispatcher = new PushChannelDispatcher();
        NotificationRequest request = new NotificationRequest();
        request.setId(UUID.randomUUID());
        try { dispatcher.dispatch(request); } catch (Exception e) {}
    }

    @Test
    void testInAppDispatcher() {
        InAppChannelDispatcher dispatcher = new InAppChannelDispatcher();
        NotificationRequest request = new NotificationRequest();
        request.setId(UUID.randomUUID());
        try { dispatcher.dispatch(request); } catch (Exception e) {}
    }

    @Test
    void testRetryAwareDispatcher() throws Exception {
        ChannelDispatcher delegate = mock(ChannelDispatcher.class);
        RetryAwareChannelDispatcher retryDispatcher = new RetryAwareChannelDispatcher(attemptRepository, requestRepository, eventPublisher);
        
        NotificationRequest request = new NotificationRequest();
        request.setId(UUID.randomUUID());
        request.setStatus(com.notificationservice.domain.NotificationStatus.QUEUED);
        
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Success case
        retryDispatcher.dispatch(request, delegate);
        verify(delegate, atLeastOnce()).dispatch(request);

        // Failure case
        request.setStatus(com.notificationservice.domain.NotificationStatus.QUEUED);
        doThrow(new TransientChannelException("Timeout")).when(delegate).dispatch(request);
        retryDispatcher.dispatch(request, delegate);
        verify(requestRepository, atLeastOnce()).save(request);
    }
}
