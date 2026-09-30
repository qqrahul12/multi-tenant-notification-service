-- V6: Notification Requests
CREATE TABLE notification_requests (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID         NOT NULL REFERENCES tenants(id),
    template_id      UUID         NOT NULL REFERENCES notification_templates(id),
    recipient_address VARCHAR(500) NOT NULL,
    variables        JSONB        NOT NULL DEFAULT '{}',
    channel          VARCHAR(20)  NOT NULL
                         CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP')),
    priority         VARCHAR(10)  NOT NULL DEFAULT 'NORMAL'
                         CHECK (priority IN ('LOW', 'NORMAL', 'HIGH')),
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING', 'QUEUED', 'DISPATCHED', 'DELIVERED', 'FAILED', 'CANCELLED')),
    scheduled_at     TIMESTAMPTZ,                   -- NULL = immediate
    idempotency_key  VARCHAR(255),
    rendered_subject VARCHAR(500),
    rendered_body    TEXT,
    attempt_count    INT          NOT NULL DEFAULT 0,
    max_attempts     INT          NOT NULL DEFAULT 3,
    created_by       UUID         REFERENCES users(id),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_nr_tenant_id       ON notification_requests(tenant_id);
CREATE INDEX idx_nr_status          ON notification_requests(status);
CREATE INDEX idx_nr_scheduled_at    ON notification_requests(scheduled_at) WHERE scheduled_at IS NOT NULL;
CREATE INDEX idx_nr_channel         ON notification_requests(channel);
CREATE INDEX idx_nr_idempotency     ON notification_requests(tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
-- Index for scheduler: pick pending scheduled notifications
CREATE INDEX idx_nr_scheduler       ON notification_requests(status, scheduled_at)
    WHERE status IN ('PENDING', 'QUEUED');
