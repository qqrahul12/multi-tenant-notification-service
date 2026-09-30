package com.notificationservice.repository;

import com.notificationservice.domain.NotificationRequest;
import com.notificationservice.domain.NotificationStatus;
import com.notificationservice.domain.Channel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, UUID> {

    Optional<NotificationRequest> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<NotificationRequest> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    Page<NotificationRequest> findByTenantId(UUID tenantId, Pageable pageable);

    Page<NotificationRequest> findByTenantIdAndStatus(UUID tenantId, NotificationStatus status, Pageable pageable);

    Page<NotificationRequest> findByTenantIdAndChannel(UUID tenantId, Channel channel, Pageable pageable);

    /**
     * Scheduler query: pick up pending scheduled notifications that are due.
     * Uses SELECT FOR UPDATE SKIP LOCKED to prevent duplicate dispatch
     * across concurrent scheduler runs (PostgreSQL distributed locking).
     */
    @Query(value = """
        SELECT * FROM notification_requests
        WHERE status = 'PENDING'
          AND (scheduled_at IS NULL OR scheduled_at <= :now)
        ORDER BY priority DESC, scheduled_at ASC NULLS FIRST
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<NotificationRequest> findPendingForDispatch(
            @Param("now") Instant now,
            @Param("batchSize") int batchSize);

    /**
     * Retry query: find QUEUED notifications that have failed attempts
     * and are due for retry.
     */
    @Query(value = """
        SELECT DISTINCT nr.* FROM notification_requests nr
        JOIN delivery_attempts da ON da.notification_request_id = nr.id
        WHERE nr.status = 'QUEUED'
          AND da.status = 'FAILURE'
          AND da.next_retry_at <= :now
          AND nr.attempt_count < nr.max_attempts
        ORDER BY nr.priority DESC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<NotificationRequest> findDueForRetry(
            @Param("now") Instant now,
            @Param("batchSize") int batchSize);

    /** Report: count by status for a tenant within a date range */
    @Query("""
        SELECT n.status, COUNT(n)
        FROM NotificationRequest n
        WHERE n.tenantId = :tenantId
          AND n.createdAt BETWEEN :from AND :to
        GROUP BY n.status
        """)
    List<Object[]> countByStatusForTenant(
            @Param("tenantId") UUID tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    /** Report: count by channel and status */
    @Query("""
        SELECT n.channel, n.status, COUNT(n)
        FROM NotificationRequest n
        WHERE n.tenantId = :tenantId
          AND n.createdAt BETWEEN :from AND :to
        GROUP BY n.channel, n.status
        """)
    List<Object[]> countByChannelAndStatusForTenant(
            @Param("tenantId") UUID tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
