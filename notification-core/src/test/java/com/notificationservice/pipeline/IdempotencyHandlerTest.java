package com.notificationservice.pipeline;

import com.notificationservice.idempotency.IdempotencyStore;
import com.notificationservice.domain.NotificationRequest;
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
class IdempotencyHandlerTest {

    @Mock private IdempotencyStore store;
    @Mock private NotificationHandlerChain chain;
    @InjectMocks private IdempotencyHandler handler;

    private NotificationContext context;
    private final UUID tenantId = UUID.randomUUID();
    private final String key = "order-123";

    @BeforeEach
    void setUp() {
        context = NotificationContext.builder()
                .tenantId(tenantId)
                .idempotencyKey(key)
                .build();
    }

    @Test
    void shouldProceedWhenKeyIsNew() {
        String redisKey = tenantId + ":" + key;
        when(store.get(redisKey)).thenReturn(Optional.empty());
        when(store.claim(eq(redisKey), any(UUID.class))).thenReturn(true);

        handler.handle(context, chain);

        verify(chain).next(context);
        assertFalse(context.isDuplicate());
    }

    @Test
    void shouldShortCircuitWhenKeyExists() {
        String redisKey = tenantId + ":" + key;
        UUID existingId = UUID.randomUUID();
        when(store.get(redisKey)).thenReturn(Optional.of(existingId));

        handler.handle(context, chain);

        verifyNoInteractions(chain);
        assertTrue(context.isDuplicate());
        assertEquals(existingId, context.getExistingNotificationId());
    }

    @Test
    void shouldProceedIfNoIdempotencyKeyProvided() {
        context.setIdempotencyKey(null);
        handler.handle(context, chain);
        verify(chain).next(context);
        verifyNoInteractions(store);
    }
}
