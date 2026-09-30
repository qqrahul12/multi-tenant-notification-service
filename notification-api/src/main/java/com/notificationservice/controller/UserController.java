package com.notificationservice.controller;

import com.notificationservice.domain.User;
import com.notificationservice.dto.request.CreateUserRequest;
import com.notificationservice.dto.request.UpdateDeviceTokenRequest;
import com.notificationservice.dto.response.ApiResponse;
import com.notificationservice.security.SecurityContextHelper;
import com.notificationservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SecurityContextHelper securityHelper;

    @PostMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, UUID>>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        User user = userService.createUser(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", Map.of("id", user.getId())));
    }

    @PutMapping("/{id}/device-token")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateDeviceToken(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeviceTokenRequest request) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        userService.updateDeviceToken(tenantId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Device token updated successfully", null));
    }
}
