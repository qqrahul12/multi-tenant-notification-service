-- V3: Tenant Rate Limit Configs
-- Separated from tenant definition (SRP — rate limiting is an operational concern)
-- Supports per-channel and per-priority limits; NULL = applies globally
CREATE TABLE tenant_rate_limit_configs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    channel          VARCHAR(20) CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP')),
    priority         VARCHAR(10) CHECK (priority IN ('LOW', 'NORMAL', 'HIGH')),
    limit_per_minute INT         NOT NULL DEFAULT 100,
    burst_capacity   INT         NOT NULL DEFAULT 200,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by       UUID        REFERENCES users(id),
    UNIQUE (tenant_id, channel, priority)
);

CREATE INDEX idx_rlc_tenant_id ON tenant_rate_limit_configs(tenant_id);
