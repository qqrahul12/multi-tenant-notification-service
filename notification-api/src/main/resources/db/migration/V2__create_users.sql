-- V2: Users
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(30)  NOT NULL
                      CHECK (role IN ('PLATFORM_ADMIN', 'TENANT_ADMIN')),
    tenant_id     UUID REFERENCES tenants(id) ON DELETE SET NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email     ON users(email);
CREATE INDEX idx_users_tenant_id ON users(tenant_id);

-- Seed: default platform admin (password: Admin@123)
INSERT INTO users (id, email, password_hash, role)
VALUES (
    gen_random_uuid(),
    'admin@platform.com',
    '$2a$12$tFkxqMsN3bKLX2RI4CXkR.GVFvfbqKUYqFOLBQpnl5W0QY7VjLpNa',
    'PLATFORM_ADMIN'
);
