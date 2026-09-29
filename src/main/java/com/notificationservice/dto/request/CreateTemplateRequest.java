package com.notificationservice.dto.request;

import com.notificationservice.domain.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTemplateRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private Channel channel;

    @Size(max = 500)
    private String subject;  // email only

    @NotBlank
    private String bodyTemplate;
}
