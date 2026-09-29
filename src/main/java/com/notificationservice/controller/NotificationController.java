package com.notificationservice.controller;

import com.notificationservice.domain.NotificationStatus;
import com.notificationservice.dto.request.BulkNotificationRequest;
import com.notificationservice.dto.request.SendNotificationRequest;
import com.notificationservice.dto.response.ApiResponse;
import com.notificationservice.dto.response.NotificationDetailResponse;
import com.notificationservice.dto.response.NotificationResponse;
import com.notificationservice.security.SecurityContextHelper;
import com.notificationservice.service.NotificationService;
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
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Tenant Admin — send and manage notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityContextHelper securityHelper;

    @PostMapping("/send")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Send or schedule a single notification (supports idempotency)")
    public ResponseEntity<ApiResponse<NotificationResponse>> send(
            @Valid @RequestBody SendNotificationRequest req) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        UUID userId = securityHelper.getCurrentUserId();
        NotificationResponse response = notificationService.send(tenantId, userId, req);
        
        HttpStatus status = response.isDuplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(ApiResponse.success(response));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Send multiple notifications in bulk")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> sendBulk(
            @Valid @RequestBody BulkNotificationRequest req) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        UUID userId = securityHelper.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(notificationService.sendBulk(tenantId, userId, req)));
    }

    @GetMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "List notifications (paginated, optional status filter)")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> list(
            @RequestParam(required = false) NotificationStatus status,
            Pageable pageable) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.list(tenantId, status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Get detailed notification status and delivery history")
    public ResponseEntity<ApiResponse<NotificationDetailResponse>> getDetail(@PathVariable UUID id) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.success(notificationService.getDetail(tenantId, id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Cancel a scheduled or pending notification")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        notificationService.cancel(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
