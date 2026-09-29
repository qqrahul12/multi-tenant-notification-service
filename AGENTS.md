# Multi-tenant Notification Service Architecture

This document describes the design decisions and architecture of the Multi-tenant Notification Service, focusing on how it meets the requirements of scalability, reliability, and clear design patterns.

## 1. Core Architecture

The system is built as a Spring Boot 3 application leveraging Java 21 Virtual Threads (`spring.threads.virtual.enabled=true`). This eliminates the need for complex reactive programming (WebFlux) while still providing massive scalability for I/O bound tasks like calling external notification providers.

### 1.1 Data Isolation (Multi-tenancy)
We use a **Logical Isolation** model. All tables contain a `tenant_id` column.
- The `SecurityContextHelper` extracts the `tenantId` from the JWT token.
- Repositories enforce `tenantId` in all queries (e.g., `findByIdAndTenantId`).
- This ensures that one tenant cannot access or modify another tenant's data.

### 1.2 Processing Pipeline (Chain of Responsibility)
When a notification is sent, it passes through the `NotificationPipeline`:
1.  **ValidationHandler:** Checks tenant status, template validity, and channel configuration.
2.  **TenantStatusHandler:** Blocks if the tenant is suspended.
3.  **IdempotencyHandler:** Uses Redis to deduplicate requests.
4.  **RateLimitHandler:** Enforces Bucket4j limits.
5.  **TemplateRenderHandler:** Injects variables into the template.
6.  **DispatchHandler:** Persists the request and either dispatches it immediately via `channelDispatchExecutor` or leaves it for the scheduler.

## 2. Key Components

### 2.1 Idempotency
- Uses Redis `SET NX` (set if not exists) with a 24-hour TTL.
- Key format: `idempotency:{tenantId}:{idempotencyKey}`.
- If a client retries a request (e.g., due to a network timeout), the `IdempotencyHandler` detects the existing key and short-circuits the pipeline, returning the existing `NotificationRequest` ID without re-sending.

### 2.2 Rate Limiting
- Uses `Bucket4j` with an in-memory `ConcurrentHashMap`.
- A hierarchical configuration model allows specific limits:
    - Specific Channel + Priority (e.g., HIGH priority SMS)
    - Channel Level (e.g., all EMAILs)
    - Tenant Global (e.g., overall API usage)
- When a tenant admin updates the rate limit configuration, a `@CacheEvict` annotation invalidates the in-memory bucket, ensuring the new limit applies on the next request.

### 2.3 Scheduling and Polling
- `@Scheduled` task runs every 30 seconds.
- Uses PostgreSQL's `SELECT ... FOR UPDATE SKIP LOCKED`.
- This is a critical feature for scalability: it allows multiple instances of the application to run concurrently. If Instance A is locking row 1 to dispatch it, Instance B will skip row 1 and pick up row 2. This acts as a robust, database-level distributed queue without needing Kafka or RabbitMQ.

### 2.4 Retry Mechanism
- The `RetryAwareChannelDispatcher` decorates the base dispatchers.
- If a `TransientChannelException` is thrown, it schedules a retry with exponential backoff (e.g., 30s, 60s, 120s) and updates the `next_retry_at` timestamp.
- The scheduler picks up failed attempts when `next_retry_at <= now()`.

## 3. Database Schema

- `tenants`, `users`
- `tenant_channel_configs` (JSONB for flexible provider config)
- `tenant_rate_limit_configs`
- `notification_templates`
- `notification_requests` (Core table, indexed on `status` and `scheduled_at`)
- `delivery_attempts` (Tracks every attempt, success or failure)
- `audit_logs` (JSONB snapshots of state changes)
