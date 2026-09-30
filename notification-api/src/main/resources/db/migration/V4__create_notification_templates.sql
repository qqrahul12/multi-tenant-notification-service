-- V4: Notification Templates
CREATE TABLE notification_templates (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    channel       VARCHAR(20)  NOT NULL
                      CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP')),
    subject       VARCHAR(500),   -- email only
    body_template TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                      CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, name, channel)
);

CREATE INDEX idx_templates_tenant_id ON notification_templates(tenant_id);
CREATE INDEX idx_templates_channel   ON notification_templates(channel);
CREATE INDEX idx_templates_status    ON notification_templates(status);
