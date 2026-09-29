package com.notificationservice.pipeline;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.Priority;
import com.notificationservice.domain.TenantRateLimitConfig;
import com.notificationservice.exception.RateLimitExceededException;
import com.notificationservice.ratelimit.BucketRateLimiter;
import com.notificationservice.ratelimit.RateLimitResult;
import com.notificationservice.repository.TenantRateLimitConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitHandlerTest {

    @Mock private BucketRateLimiter rateLimiter;
    @Mock private TenantRateLimitConfigRepository configRepository;
    @Mock private NotificationHandlerChain chain;

    @InjectMocks private RateLimitHandler rateLimitHandler;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimitHandler, "defaultPerMinute", 100);
    }

    @Test
    void shouldAllowWhenUnderLimit() {
        NotificationContext context = NotificationContext.builder()
                .tenantId(tenantId)
                .channel(Channel.EMAIL)
                .priority(Priority.HIGH)
                .build();

        TenantRateLimitConfig config = new TenantRateLimitConfig();
        config.setLimitPerMinute(50);
        config.setBurstCapacity(100);

        when(configRepository.findByTenantIdAndChannelAndPriority(tenantId, Channel.EMAIL, Priority.HIGH))
                .thenReturn(Optional.of(config));
        
        when(rateLimiter.tryConsume(tenantId + ":EMAIL:HIGH", 50, 100))
                .thenReturn(RateLimitResult.allowed(49));

        rateLimitHandler.handle(context, chain);
        verify(chain).next(context);
    }

    @Test
    void shouldThrowRateLimitExceededWhenDenied() {
        NotificationContext context = NotificationContext.builder()
                .tenantId(tenantId)
                .channel(Channel.SMS)
                .priority(Priority.NORMAL)
                .build();

        when(configRepository.findByTenantIdAndChannelAndPriority(tenantId, Channel.SMS, Priority.NORMAL))
                .thenReturn(Optional.empty()); // Will fall back through hierarchy
        when(configRepository.findByTenantIdAndChannelAndPriorityIsNull(tenantId, Channel.SMS))
                .thenReturn(Optional.empty());
        when(configRepository.findByTenantIdAndChannelIsNullAndPriorityIsNull(tenantId))
                .thenReturn(Optional.empty());

        // Uses default limits: 100 limit, 200 burst
        when(rateLimiter.tryConsume(tenantId + ":SMS:NORMAL", 100, 200))
                .thenReturn(RateLimitResult.denied(Duration.ofSeconds(10)));

        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class, 
            () -> rateLimitHandler.handle(context, chain));
        
        assertEquals(10, ex.getRetryAfterSeconds());
        verifyNoInteractions(chain);
    }
}
