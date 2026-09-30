package com.notificationservice.repository;

import com.notificationservice.domain.Tenant;
import com.notificationservice.domain.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<Tenant> findByStatus(TenantStatus status, Pageable pageable);
}
