package com.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * Bounded ThreadPoolExecutor for channel dispatching as per assignment requirements:
     * "dispatch high volumes of notifications across channels concurrently using bounded worker pools".
     *
     * Core Size: 50
     * Max Size: 50
     * Queue: ArrayBlockingQueue (5000 limit) - prevents OutOfMemory on huge traffic spikes.
     * Rejection Policy: CallerRunsPolicy - slows down the publisher (backpressure) if the queue is full.
     */
    @Bean(name = "channelDispatchExecutor")
    public ExecutorService channelDispatchExecutor() {
        return new ThreadPoolExecutor(
                50, 
                50, 
                0L, 
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(5000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
