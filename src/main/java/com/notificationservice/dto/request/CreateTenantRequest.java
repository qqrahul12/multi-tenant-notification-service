package com.notificationservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateTenantRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be lowercase alphanumeric with hyphens only")
    @Size(max = 100)
    private String slug;
}
