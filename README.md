# Multi-tenant Notification Service

A scalable, robust, and idempotent multi-tenant notification service built with Spring Boot 3, Java 21 (Virtual Threads), PostgreSQL, and Redis.

## Features

*   **Multi-tenancy:** Strict data isolation per tenant across all APIs and data layers.
*   **Channels:** Configurable dispatch across 4 channels (Email, SMS, Push, In-App).
*   **Virtual Threads:** Uses Java 21 Virtual Threads for high-throughput, non-blocking I/O during channel dispatch via bounded worker pools.
*   **Smart Rate Limiting:** In-memory Bucket4j rate limiting with fallback hierarchy (specific channel/priority -> channel -> global tenant -> default).
*   **Idempotency:** Redis `SET NX` based idempotency to prevent duplicate notifications on client retries.
*   **Reliable Delivery:** Exponential backoff retries for transient failures, tracked via `delivery_attempts`.
*   **Scheduled Sends:** Scalable polling scheduler using PostgreSQL `SELECT FOR UPDATE SKIP LOCKED` to prevent duplicate dispatches across instances.
*   **Templating:** Dynamic template rendering using `{{variable}}` substitution.
*   **Role-Based Access Control:** JWT-based authentication with `PLATFORM_ADMIN`, `TENANT_ADMIN`, and `USER` roles.
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

## API Documentation (Swagger)

Start the application and navigate to `http://localhost:8080/swagger-ui.html`. 
The Swagger UI includes a built-in "Authorize" button to provide the JWT token (acquired via `/api/v1/auth/login`).

## Architecture & Assumptions

* **Entities:** Core models include Tenants, Users, Templates, Channel Configurations, Notification Requests, and Delivery Attempts.
* **Worker Pools:** Channel dispatches are managed via a ThreadPoolExecutor backed by Java 21 Virtual Threads to meet the "bounded worker pool" requirement without thread starvation.
* **In-App Channel:** Handled by broadcasting notifications over Redis Pub/Sub (`InAppChannelDispatcher`). A WebSocket frontend server can subscribe to this topic to push events directly to clients.
* **Data Isolation:** All operations mandate a `tenantId` (derived automatically via JWT Security Context).

## Getting Started

1.  **Start Dependencies:**
    ```bash
    docker-compose up -d
    ```

2.  **Run the Application:**
    ```bash
    ./mvnw spring-boot:run
    ```

## Default Credentials

A platform admin is created automatically by Flyway migrations (`V9__seed_platform_admin.sql`):
*   **Email:** `admin@platform.com`
*   **Password:** `admin123`
