package com.notificationservice.controller;

import com.notificationservice.domain.NotificationTemplate;
import com.notificationservice.domain.TemplateStatus;
import com.notificationservice.dto.request.CreateTemplateRequest;
import com.notificationservice.dto.response.ApiResponse;
import com.notificationservice.security.SecurityContextHelper;
import com.notificationservice.service.TemplateService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Templates", description = "Tenant Admin — manage notification templates")
@SecurityRequirement(name = "bearerAuth")
public class TemplateController {

    private final TemplateService templateService;
    private final SecurityContextHelper securityHelper;

    @PostMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Create a notification template with {{variable}} support")
    public ResponseEntity<ApiResponse<NotificationTemplate>> create(
            @Valid @RequestBody CreateTemplateRequest req) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(templateService.createTemplate(tenantId, req)));
    }

    @GetMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "List templates for the current tenant (paginated)")
    public ResponseEntity<ApiResponse<Page<NotificationTemplate>>> list(
            @RequestParam(required = false) TemplateStatus status,
            Pageable pageable) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.success(
                templateService.listTemplates(tenantId, status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Get a template by ID")
    public ResponseEntity<ApiResponse<NotificationTemplate>> get(@PathVariable UUID id) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.success(templateService.getTemplate(tenantId, id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Update a template")
    public ResponseEntity<ApiResponse<NotificationTemplate>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTemplateRequest req) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.success(templateService.updateTemplate(tenantId, id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Soft-delete a template (sets status to INACTIVE)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        templateService.deleteTemplate(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
