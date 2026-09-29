# Multi-tenant Notification Service

A scalable, robust, and idempotent multi-tenant notification service built with Spring Boot 3, Java 21 (Virtual Threads), PostgreSQL, and Redis.

## Features

*   **Multi-tenancy:** Strict data isolation per tenant across all APIs and data layers.
*   **Virtual Threads:** Uses Java 21 Virtual Threads for high-throughput, non-blocking I/O during channel dispatch.
*   **Smart Rate Limiting:** In-memory Bucket4j rate limiting with fallback hierarchy (specific channel/priority -> channel -> global tenant -> default).
*   **Idempotency:** Redis `SET NX` based idempotency to prevent duplicate notifications on client retries.
*   **Reliable Delivery:** Exponential backoff retries for transient failures, tracked via `delivery_attempts`.
*   **Scheduled Sends:** Scalable polling scheduler using PostgreSQL `SELECT FOR UPDATE SKIP LOCKED` to prevent duplicate dispatches across instances.
*   **Templating:** Dynamic template rendering using `{{variable}}` substitution.
*   **Role-Based Access Control:** JWT-based authentication with `PLATFORM_ADMIN` and `TENANT_ADMIN` roles.
*   **Auditing:** Async domain events trigger JSONB audit logging for full traceability of notification state changes.

## Tech Stack

*   Java 21
*   Spring Boot 3.3.4
*   PostgreSQL 16
*   Redis 7
*   Bucket4j (Rate Limiting)
*   JJWT (Authentication)
*   Flyway (Database Migrations)
*   Swagger / Springdoc (API Documentation)
*   Docker & Docker Compose

## Design Patterns Used

1.  **Strategy:** `ChannelDispatcher` interface for different notification channels.
2.  **Factory:** `ChannelDispatcherFactory` provides the correct dispatcher at runtime.
3.  **Template Method:** `AbstractChannelDispatcher` defines the dispatch skeleton.
4.  **Chain of Responsibility:** `NotificationPipeline` with sequential validation, rate limit, idempotency, and dispatch handlers.
5.  **Decorator:** `RetryAwareChannelDispatcher` adds retry logic transparently over standard dispatchers.
6.  **State:** `NotificationStatus` manages valid lifecycle transitions.
7.  **Observer:** `AuditLogEventListener` listens to domain events for async logging.
8.  **Command:** `NotificationDispatchCommand` encapsulates dispatch requests for virtual thread execution.
9.  **Builder:** Fluent object creation using Lombok `@Builder`.
10. **Facade:** `NotificationService` provides a unified entry point for controllers.

## Getting Started

1.  **Start Dependencies:**
    ```bash
    docker-compose up -d
    ```

2.  **Run the Application:**
    ```bash
    ./mvnw spring-boot:run
    ```

3.  **API Documentation (Swagger UI):**
    Open `http://localhost:8080/swagger-ui.html` in your browser.

## Default Credentials

A platform admin is created automatically by Flyway migrations:
*   **Email:** `admin@platform.com`
*   **Password:** `Admin@123`
