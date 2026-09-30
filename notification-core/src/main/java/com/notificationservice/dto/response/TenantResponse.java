package com.notificationservice.dto.response;

import com.notificationservice.domain.Tenant;
import com.notificationservice.domain.TenantStatus;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TenantResponse {
    private UUID id;
    private String name;
    private String slug;
    private TenantStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static TenantResponse from(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .status(tenant.getStatus())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }
}
