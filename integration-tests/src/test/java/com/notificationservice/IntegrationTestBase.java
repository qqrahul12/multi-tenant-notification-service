package com.notificationservice;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base configuration for all integration tests.
 * Explicitly references NotificationServiceApplication (in notification-api module)
 * as the Spring Boot entry point, since integration-tests has no @SpringBootApplication.
 */
@SpringBootTest(
    classes = NotificationServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public abstract class IntegrationTestBase {
}
