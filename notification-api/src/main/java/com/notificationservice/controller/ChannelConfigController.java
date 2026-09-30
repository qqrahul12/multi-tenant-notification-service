package com.notificationservice.controller;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.NotificationChannelConfig;
import com.notificationservice.dto.request.UpsertChannelConfigRequest;
import com.notificationservice.dto.response.ApiResponse;
import com.notificationservice.security.SecurityContextHelper;
import com.notificationservice.service.ChannelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/channel-configs")
@RequiredArgsConstructor
@Tag(name = "Channel Config", description = "Tenant Admin — configure notification channels")
@SecurityRequirement(name = "bearerAuth")
public class ChannelConfigController {

    private final ChannelConfigService channelConfigService;
    private final SecurityContextHelper securityHelper;

    @PutMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Upsert channel configuration (EMAIL, SMS, PUSH, IN_APP)")
    public ResponseEntity<ApiResponse<NotificationChannelConfig>> upsert(
            @Valid @RequestBody UpsertChannelConfigRequest req) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.success(channelConfigService.upsert(tenantId, req)));
    }

    @GetMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "List all channel configs for the current tenant")
    public ResponseEntity<ApiResponse<List<NotificationChannelConfig>>> list() {
        UUID tenantId = securityHelper.getCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.success(channelConfigService.listConfigs(tenantId)));
    }

    @GetMapping("/{channel}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Get config for a specific channel")
    public ResponseEntity<ApiResponse<NotificationChannelConfig>> get(@PathVariable Channel channel) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.success(channelConfigService.getConfig(tenantId, channel)));
    }
}
