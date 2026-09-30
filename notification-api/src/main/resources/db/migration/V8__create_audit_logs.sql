-- V8: Audit Logs
CREATE TABLE audit_logs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID        REFERENCES tenants(id),
    entity_type  VARCHAR(100) NOT NULL,
    entity_id    UUID        NOT NULL,
    action       VARCHAR(100) NOT NULL,
    performed_by UUID        REFERENCES users(id),
    old_state    JSONB,
    new_state    JSONB,
    metadata     JSONB,
    ip_address   VARCHAR(45),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_entity      ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_tenant_id   ON audit_logs(tenant_id);
CREATE INDEX idx_audit_performed_by ON audit_logs(performed_by);
CREATE INDEX idx_audit_created_at  ON audit_logs(created_at);
