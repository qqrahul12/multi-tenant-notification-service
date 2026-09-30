package com.notificationservice.service;

import com.notificationservice.domain.Tenant;
import com.notificationservice.domain.TenantRateLimitConfig;
import com.notificationservice.domain.TenantStatus;
import com.notificationservice.dto.request.CreateTenantRequest;
import com.notificationservice.dto.request.UpsertRateLimitRequest;
import com.notificationservice.dto.response.TenantResponse;
import com.notificationservice.exception.TenantNotFoundException;
import com.notificationservice.ratelimit.BucketRateLimiter;
import com.notificationservice.repository.TenantRateLimitConfigRepository;
import com.notificationservice.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantRateLimitConfigRepository rateLimitConfigRepository;
    private final BucketRateLimiter rateLimiter;

    @Transactional
    public TenantResponse createTenant(CreateTenantRequest req) {
        if (tenantRepository.existsBySlug(req.getSlug())) {
            throw new IllegalArgumentException("Tenant slug already exists: " + req.getSlug());
        }
        Tenant tenant = Tenant.builder()
                .name(req.getName())
                .slug(req.getSlug())
                .build();
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    @Transactional(readOnly = true)
    public Page<TenantResponse> listTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable).map(TenantResponse::from);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tenants", key = "#tenantId")
    public TenantResponse getTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(TenantResponse::from)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));
    }

    @Transactional
    @CacheEvict(value = "tenants", key = "#tenantId")
    public TenantResponse updateStatus(UUID tenantId, TenantStatus newStatus) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));
        tenant.setStatus(newStatus);
        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant {} status changed to {}", tenantId, newStatus);
        return TenantResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TenantRateLimitConfig> getRateLimitConfigs(UUID tenantId) {
        return rateLimitConfigRepository.findByTenantId(tenantId);
    }

    @Transactional
    @CacheEvict(value = "rateLimitConfig", key = "#tenantId")
    public TenantRateLimitConfig upsertRateLimitConfig(UUID tenantId, UUID updatedBy,
                                                        UpsertRateLimitRequest req) {
        // Find existing or create new
        TenantRateLimitConfig config = rateLimitConfigRepository
                .findByTenantIdAndChannelAndPriority(tenantId, req.getChannel(), req.getPriority())
                .orElseGet(() -> TenantRateLimitConfig.builder()
                        .tenantId(tenantId)
                        .channel(req.getChannel())
                        .priority(req.getPriority())
                        .build());

        config.setLimitPerMinute(req.getLimitPerMinute());
        config.setBurstCapacity(req.getBurstCapacity());
        config.setUpdatedBy(updatedBy);

        TenantRateLimitConfig saved = rateLimitConfigRepository.save(config);

        // Invalidate in-memory rate limit bucket so new config takes effect immediately
        rateLimiter.invalidateAllForTenant(tenantId.toString());

        log.info("Rate limit config updated for tenant={} channel={} priority={} limit={}",
                tenantId, req.getChannel(), req.getPriority(), req.getLimitPerMinute());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "tenants", key = "#tenantId")
    public void deleteTenant(UUID tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new TenantNotFoundException("Tenant not found: " + tenantId);
        }
        tenantRepository.deleteById(tenantId);
    }
}
