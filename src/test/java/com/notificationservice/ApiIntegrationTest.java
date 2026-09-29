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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@org.junit.jupiter.api.Disabled("Requires Docker running (Testcontainers)")
class ApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private ObjectMapper objectMapper;

    private String tenantToken;
    private UUID tenantId;

    @BeforeEach
    void setup() {
        tenantId = UUID.randomUUID();
        com.notificationservice.domain.User user = new com.notificationservice.domain.User();
        user.setId(UUID.randomUUID());
        user.setTenantId(tenantId);
        user.setRole(com.notificationservice.domain.UserRole.TENANT_ADMIN);
        
        com.notificationservice.security.UserPrincipal principal = new com.notificationservice.security.UserPrincipal(user);
        tenantToken = jwtUtil.generateToken(principal);
    }

    @Test
    void shouldRejectUnauthorizedRequests() throws Exception {
        SendNotificationRequest request = new SendNotificationRequest();
        
        mockMvc.perform(post("/api/v1/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
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
