package com.notificationservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Stores channel-specific provider configuration per tenant.
 * One tenant can have multiple channel configs (EMAIL, SMS, PUSH, IN_APP).
 * The JSONB config blob keeps it flexible for any provider's settings.
 */
@Entity
@Table(name = "tenant_channel_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationChannelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    /**
     * JSONB blob for channel-specific config:
     * EMAIL : { "fromAddress": "noreply@acme.com", "fromName": "Acme" }
     * SMS   : { "senderId": "ACME", "provider": "TWILIO" }
     * PUSH  : { "fcmServerKey": "...", "apnsCertificate": "..." }
     * IN_APP: { "appId": "..." }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> config = Map.of();

    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    private Instant updatedAt;
}
