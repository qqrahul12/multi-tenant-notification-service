---
name: springboot-multi-tenant-generator
description: A specialized skill for generating highly scalable, multi-tenant Spring Boot microservices with rate-limiting, virtual threads, and Redis idempotency.
---

# Spring Boot Multi-tenant Notification Service Generator

This skill was used by the Antigravity AI to generate the Multi-tenant Notification Service.

## Architectural Rules Followed

1.  **Concurrency**: Implemented exactly as requested: "bounded worker pools" via `ThreadPoolExecutor` with a strict `ArrayBlockingQueue` and `CallerRunsPolicy` to enforce system-wide concurrency limits and prevent OutOfMemory crashes under massive load.
2.  **Design Patterns**:
    *   **Chain of Responsibility**: Used in `NotificationPipeline` to process validation, tenant status checks, idempotency, rate-limiting, and template rendering sequentially.
    *   **Strategy Pattern**: Used via `ChannelDispatcher` interface and its concrete implementations (Email, SMS, Push, In-App).
    *   **Factory Pattern**: Used in `ChannelDispatcherFactory` to retrieve the correct dispatcher based on the `Channel` enum.
    *   **Decorator Pattern**: Used in `RetryAwareChannelDispatcher` to wrap standard dispatchers with retry-with-backoff logic.
3.  **Data Persistence**:
    *   **PostgreSQL**: Used for transactional data (Tenants, Templates, Configurations, Audit Logs).
    *   **Redis**: Used as a distributed cache for `IdempotencyStore` to prevent duplicate processing of the same `idempotencyKey` across multiple horizontally scaled instances.
4.  **Rate Limiting**:
    *   Implemented using `Bucket4j`.
    *   Hierarchical limits: Fallback from specific channel/priority limits down to global tenant limits.

## Prompts & Directives Used

*   "Use Java 21, let's try using virtual threads if possible."
*   "RateLimitPerMinute - this should be a separate tenant config instead of tenant definition."
*   "Use SOLID principles and various design patterns."
*   "I need all scenarios covered with both integration tests and unit tests."
