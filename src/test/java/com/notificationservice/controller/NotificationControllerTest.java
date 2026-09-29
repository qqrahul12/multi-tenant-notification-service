package com.notificationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationservice.domain.Channel;
import com.notificationservice.dto.request.SendNotificationRequest;
import com.notificationservice.dto.response.NotificationResponse;
import com.notificationservice.security.JwtUtil;
import com.notificationservice.security.SecurityContextHelper;
import com.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit test to focus on controller logic
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private NotificationService notificationService;
    @MockBean private SecurityContextHelper securityHelper;
    @MockBean private JwtUtil jwtUtil; // Required for context load
    @MockBean private com.notificationservice.repository.UserRepository userRepository;

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void shouldSendNotification() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(securityHelper.getCurrentUserId()).thenReturn(userId);

        SendNotificationRequest req = new SendNotificationRequest();
        req.setTemplateId(UUID.randomUUID());
        req.setChannel(Channel.EMAIL);
        req.setRecipientAddress("user@example.com");

        NotificationResponse resp = NotificationResponse.builder()
                .id(notificationId)
                .isDuplicate(false)
                .build();

        when(notificationService.send(eq(tenantId), eq(userId), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(notificationId.toString()));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void shouldReturnOkForDuplicateNotification() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(securityHelper.getCurrentUserId()).thenReturn(userId);

        SendNotificationRequest req = new SendNotificationRequest();
        req.setTemplateId(UUID.randomUUID());
        req.setChannel(Channel.EMAIL);
        req.setRecipientAddress("user@example.com");

        NotificationResponse resp = NotificationResponse.builder()
                .id(notificationId)
                .isDuplicate(true) // Marks as duplicate
                .build();

        when(notificationService.send(eq(tenantId), eq(userId), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()) // Expect 200 OK instead of 201 Created
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.duplicate").value(true));
    }
}
