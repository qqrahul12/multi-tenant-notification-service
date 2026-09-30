package com.notificationservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class BulkNotificationRequest {
    @NotEmpty
    @Valid
    private List<SendNotificationRequest> notifications;
}
