package com.notificationservice.controller;

import com.notificationservice.domain.TenantRateLimitConfig;
import com.notificationservice.domain.TenantStatus;
import com.notificationservice.dto.request.CreateTenantRequest;
import com.notificationservice.dto.request.UpsertRateLimitRequest;
import com.notificationservice.dto.response.ApiResponse;
import com.notificationservice.dto.response.TenantResponse;
import com.notificationservice.security.SecurityContextHelper;
import com.notificationservice.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management", description = "Platform Admin only — manage tenants and rate limits")
@SecurityRequirement(name = "bearerAuth")
public class TenantController {

    private final TenantService tenantService;
    private final SecurityContextHelper securityHelper;

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create a new tenant")
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(
            @Valid @RequestBody CreateTenantRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tenantService.createTenant(req)));
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List all tenants (paginated)")
    public ResponseEntity<ApiResponse<Page<TenantResponse>>> listTenants(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(tenantService.listTenants(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Get tenant by ID")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(tenantService.getTenant(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update tenant status (ACTIVE/SUSPENDED)")
    public ResponseEntity<ApiResponse<TenantResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam TenantStatus status) {
        return ResponseEntity.ok(ApiResponse.success(tenantService.updateStatus(id, status)));
    }

    @GetMapping("/{id}/rate-limits")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Get all rate limit configs for a tenant")
    public ResponseEntity<ApiResponse<List<TenantRateLimitConfig>>> getRateLimits(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(tenantService.getRateLimitConfigs(id)));
    }

    @PutMapping("/{id}/rate-limits")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Upsert rate limit config for a tenant (per channel/priority)")
    public ResponseEntity<ApiResponse<TenantRateLimitConfig>> upsertRateLimit(
            @PathVariable UUID id,
            @Valid @RequestBody UpsertRateLimitRequest req) {
        UUID userId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                tenantService.upsertRateLimitConfig(id, userId, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Delete a tenant")
    public ResponseEntity<Void> deleteTenant(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.noContent().build();
    }
}
