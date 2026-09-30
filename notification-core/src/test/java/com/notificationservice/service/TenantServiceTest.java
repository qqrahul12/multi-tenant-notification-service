package com.notificationservice.service;

import com.notificationservice.domain.Tenant;
import com.notificationservice.domain.TenantStatus;
import com.notificationservice.dto.request.CreateTenantRequest;
import com.notificationservice.dto.response.TenantResponse;
import com.notificationservice.exception.TenantNotFoundException;
import com.notificationservice.ratelimit.BucketRateLimiter;
import com.notificationservice.repository.TenantRateLimitConfigRepository;
import com.notificationservice.repository.TenantRepository;
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
class TenantServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantRateLimitConfigRepository rateLimitConfigRepository;
    @Mock private BucketRateLimiter rateLimiter;

    @InjectMocks private TenantService tenantService;

    @Test
    void shouldCreateTenantSuccessfully() {
        CreateTenantRequest req = new CreateTenantRequest();
        req.setName("Test Tenant");
        req.setSlug("test-tenant");

        when(tenantRepository.existsBySlug("test-tenant")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> {
            Tenant t = i.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TenantResponse response = tenantService.createTenant(req);
        assertNotNull(response.getId());
        assertEquals("Test Tenant", response.getName());
    }

    @Test
    void shouldThrowWhenSlugExists() {
        CreateTenantRequest req = new CreateTenantRequest();
        req.setSlug("exists");

        when(tenantRepository.existsBySlug("exists")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> tenantService.createTenant(req));
    }

    @Test
    void shouldUpdateStatus() {
        UUID id = UUID.randomUUID();
        Tenant t = new Tenant();
        t.setId(id);
        t.setStatus(TenantStatus.ACTIVE);

        when(tenantRepository.findById(id)).thenReturn(Optional.of(t));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(t);

        TenantResponse resp = tenantService.updateStatus(id, TenantStatus.SUSPENDED);
        assertEquals(TenantStatus.SUSPENDED, resp.getStatus());
    }
}
