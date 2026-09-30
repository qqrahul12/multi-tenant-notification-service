package com.notificationservice.service;

import com.notificationservice.repository.NotificationRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private NotificationRequestRepository requestRepository;
    @InjectMocks private ReportService reportService;

    @Test
    void shouldGetSummary() {
        UUID tenantId = UUID.randomUUID();
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();

        List<Object[]> byStatus = List.of(
            new Object[]{"DELIVERED", 80L},
            new Object[]{"FAILED", 20L}
        );

        List<Object[]> byChannel = List.of(
            new Object[]{"EMAIL", "DELIVERED", 50L},
            new Object[]{"EMAIL", "FAILED", 10L},
            new Object[]{"SMS", "DELIVERED", 30L},
            new Object[]{"SMS", "FAILED", 10L}
        );

        when(requestRepository.countByStatusForTenant(tenantId, from, to)).thenReturn(byStatus);
        when(requestRepository.countByChannelAndStatusForTenant(tenantId, from, to)).thenReturn(byChannel);

        Map<String, Object> summary = reportService.getSummary(tenantId, from, to);

        assertEquals(100L, summary.get("total"));
        assertEquals("80.00%", summary.get("deliveryRate"));
        
        @SuppressWarnings("unchecked")
        Map<String, Long> statusCounts = (Map<String, Long>) summary.get("byStatus");
        assertEquals(80L, statusCounts.get("DELIVERED"));
        assertEquals(20L, statusCounts.get("FAILED"));
    }
}
