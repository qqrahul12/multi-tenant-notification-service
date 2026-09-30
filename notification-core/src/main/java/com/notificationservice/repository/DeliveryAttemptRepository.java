package com.notificationservice.repository;

import com.notificationservice.domain.DeliveryAttempt;
import com.notificationservice.domain.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {

    List<DeliveryAttempt> findByNotificationRequestIdOrderByAttemptNumberAsc(UUID notificationRequestId);

    Optional<DeliveryAttempt> findTopByNotificationRequestIdOrderByAttemptNumberDesc(UUID notificationRequestId);

    int countByNotificationRequestId(UUID notificationRequestId);

    List<DeliveryAttempt> findByNotificationRequestIdAndStatus(UUID notificationRequestId, AttemptStatus status);
}
