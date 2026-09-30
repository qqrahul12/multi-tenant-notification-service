-- Insert the platform tenant
INSERT INTO tenants (id, name, slug, status, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Platform Admin Tenant',
    'platform',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Insert the platform admin user
-- Password is 'admin123' hashed with BCrypt
INSERT INTO users (id, tenant_id, email, password_hash, role, enabled, created_at, updated_at)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'admin@platform.com',
    '$2a$10$w0M0PZ1y33V5w/V3P7P/0ehv4/wR.v9jAIfYfLqRIf9wXp/k.V28.', 
    'PLATFORM_ADMIN',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
