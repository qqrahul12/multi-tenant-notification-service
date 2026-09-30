package com.notificationservice.service;

import com.notificationservice.repository.NotificationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final NotificationRequestRepository requestRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(UUID tenantId, Instant from, Instant to) {
        List<Object[]> byStatus  = requestRepository.countByStatusForTenant(tenantId, from, to);
        List<Object[]> byChannel = requestRepository.countByChannelAndStatusForTenant(tenantId, from, to);

        Map<String, Long> statusCounts  = new LinkedHashMap<>();
        Map<String, Map<String, Long>> channelBreakdown = new LinkedHashMap<>();

        long total = 0;
        for (Object[] row : byStatus) {
            String status = row[0].toString();
            long count = ((Number) row[1]).longValue();
            statusCounts.put(status, count);
            total += count;
        }

        for (Object[] row : byChannel) {
            String channel = row[0].toString();
            String status  = row[1].toString();
            long   count   = ((Number) row[2]).longValue();
            channelBreakdown.computeIfAbsent(channel, k -> new LinkedHashMap<>()).put(status, count);
        }

        long delivered = statusCounts.getOrDefault("DELIVERED", 0L);
        double deliveryRate = total > 0 ? (double) delivered / total * 100 : 0.0;

        return Map.of(
                "tenantId",        tenantId,
                "from",            from,
                "to",              to,
                "total",           total,
                "deliveryRate",    String.format("%.2f%%", deliveryRate),
                "byStatus",        statusCounts,
                "byChannel",       channelBreakdown
        );
    }
}
