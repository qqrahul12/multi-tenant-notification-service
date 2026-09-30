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

## 4. AI Workflow & Skills Used

This project was developed iteratively using the **Google Antigravity (AGY) Agent** (Gemini 3.1 Pro / Claude 3.5 Sonnet hybrid). 

### 4.1 AI Workflow
1. **Scoping & Setup:** The agent was provided with the raw PDF requirements. It iteratively split the problem into domain modeling, REST API definition, and pipeline architecture.
2. **Iterative Implementation:**
   - Multi-module Maven setup was driven by the agent.
   - Flyway migrations were written sequentially by the agent to incrementally build the schema.
   - Core pipeline (Chain of Responsibility) was scaffolded and refined via conversational prompts.
3. **Refactoring & Bug Fixing:**
   - The agent ran native terminal commands (`mvn test`) to identify compilation errors and failed tests, parsing stack traces to fix issues (e.g., resolving the PostgreSQL `DISTINCT FOR UPDATE` bug by rewriting to `EXISTS`).
4. **Testing:** The agent generated unit tests and utilized `Testcontainers` / `GreenMail` for real SMTP and database integration tests.

### 4.2 AI Agent Skills Utilized
- **Terminal & Shell Execution:** Extensively used `run_command` to execute Maven builds, Git commands, and run Python scripts to parse assignment PDFs.
- **Code Editing:** Used `replace_file_content` and `write_to_file` to precisely inject logic without rewriting entire classes (e.g., modifying `UserController.java` or `pom.xml`).
- **File System Navigation:** Used `find`, `grep`, and `cat` to understand the codebase context (e.g., verifying `ThreadPoolExecutor` implementations across the project).
- **Artifact Management:** Created and managed `progress_report.md` to track assignment completion status against the original prompt requirements.
