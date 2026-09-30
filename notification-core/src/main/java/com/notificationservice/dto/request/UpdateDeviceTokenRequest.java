package com.notificationservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateDeviceTokenRequest {
    @NotBlank(message = "Device token is required")
    private String deviceToken;
}
