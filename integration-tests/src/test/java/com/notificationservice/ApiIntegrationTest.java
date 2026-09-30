package com.notificationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationservice.domain.Channel;
import com.notificationservice.dto.request.SendNotificationRequest;
import com.notificationservice.repository.NotificationRequestRepository;
import com.notificationservice.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NotificationServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private ObjectMapper objectMapper;

    private String tenantToken;
    private UUID tenantId;

    @Autowired private com.notificationservice.repository.UserRepository userRepository;
    @Autowired private com.notificationservice.repository.TenantRepository tenantRepository;
    @Autowired private com.notificationservice.repository.NotificationRequestRepository requestRepository;
    @Autowired private com.notificationservice.repository.AuditLogRepository auditLogRepository;

    @BeforeEach
    void setup() {
        requestRepository.deleteAll();
        userRepository.deleteAll();
        auditLogRepository.deleteAll();
        tenantRepository.deleteAll();

        com.notificationservice.domain.Tenant tenant = com.notificationservice.domain.Tenant.builder()
                .name("Test Tenant")
                .slug("test")
                .status(com.notificationservice.domain.TenantStatus.ACTIVE)
                .build();
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();

        com.notificationservice.domain.User user = new com.notificationservice.domain.User();
        user.setId(UUID.randomUUID());
        user.setTenantId(tenantId);
        user.setEmail("admin@test.com");
        user.setPasswordHash("hash");
        user.setRole(com.notificationservice.domain.UserRole.TENANT_ADMIN);
        user = userRepository.save(user);
        
        com.notificationservice.security.UserPrincipal principal = new com.notificationservice.security.UserPrincipal(user);
        tenantToken = jwtUtil.generateToken(principal);
    }

    @Test
    void shouldRejectUnauthorizedRequests() throws Exception {
        SendNotificationRequest request = new SendNotificationRequest();
        
        mockMvc.perform(post("/api/v1/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnBadRequestForInvalidPayload() throws Exception {
        SendNotificationRequest request = new SendNotificationRequest(); // Missing required fields
        
        mockMvc.perform(post("/api/v1/notifications/send")
                .header("Authorization", "Bearer " + tenantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
