package com.notificationservice.controller;

import com.notificationservice.dto.response.ApiResponse;
import com.notificationservice.security.SecurityContextHelper;
import com.notificationservice.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Tenant Admin — analytics and delivery reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;
    private final SecurityContextHelper securityHelper;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Get delivery statistics and breakdown by channel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.success(reportService.getSummary(tenantId, from, to)));
    }
}
