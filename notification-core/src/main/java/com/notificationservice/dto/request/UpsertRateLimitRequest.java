package com.notificationservice.dto.request;

import com.notificationservice.domain.Channel;
import com.notificationservice.domain.Priority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpsertRateLimitRequest {

    /** null = global limit for all channels */
    private Channel channel;

    /** null = applies to all priorities */
    private Priority priority;

    @NotNull
    @Min(1)
    private Integer limitPerMinute;

    @NotNull
    @Min(1)
    private Integer burstCapacity;
}
