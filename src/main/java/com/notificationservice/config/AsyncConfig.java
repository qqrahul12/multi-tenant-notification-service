package com.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Virtual Threads configuration for Java 21.
 *
 * Spring Boot 3.2+ with spring.threads.virtual.enabled=true automatically
 * applies virtual threads to:
 *  - Tomcat request threads (every HTTP request runs on a virtual thread)
 *  - @Async methods
 *  - Spring's TaskExecutor
 *
 * Here we additionally provide a dedicated virtual thread executor for
 * channel dispatch operations — I/O-bound work is ideal for virtual threads
 * as they park (not block platform threads) during network I/O.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * Virtual thread executor for channel dispatching.
     * Unlike platform thread pools, virtual threads are cheap — no pool size needed.
     * We use a Semaphore in the dispatcher for concurrency bounding instead.
     */
    @Bean(name = "channelDispatchExecutor")
    public ExecutorService channelDispatchExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
