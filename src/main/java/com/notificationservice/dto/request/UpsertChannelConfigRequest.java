package com.notificationservice.dto.request;

import com.notificationservice.domain.Channel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class UpsertChannelConfigRequest {
    @NotNull
    private Channel channel;
    private Map<String, String> config;
    private boolean enabled = true;
}
