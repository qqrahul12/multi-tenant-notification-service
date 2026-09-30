-- V5: Tenant Channel Configs
CREATE TABLE tenant_channel_configs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    channel     VARCHAR(20) NOT NULL
                    CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP')),
    config      JSONB       NOT NULL DEFAULT '{}',   -- API keys, from address, etc.
    enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, channel)
);

CREATE INDEX idx_channel_configs_tenant_id ON tenant_channel_configs(tenant_id);
