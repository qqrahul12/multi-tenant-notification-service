package com.notificationservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_rate_limit_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantRateLimitConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    /**
     * NULL means this config applies globally to all channels.
     * A specific channel overrides the global config.
     */
    @Enumerated(EnumType.STRING)
    private Channel channel;

    /**
     * NULL means this config applies to all priorities.
     * A specific priority overrides the global config.
     */
    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(nullable = false)
    @Builder.Default
    private int limitPerMinute = 100;

    @Column(nullable = false)
    @Builder.Default
    private int burstCapacity = 200;

    @UpdateTimestamp
    private Instant updatedAt;

    private UUID updatedBy;
}
