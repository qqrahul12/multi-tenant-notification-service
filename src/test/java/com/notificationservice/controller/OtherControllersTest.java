package com.notificationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationservice.domain.Channel;
import com.notificationservice.domain.NotificationTemplate;
import com.notificationservice.domain.TenantRateLimitConfig;
import com.notificationservice.dto.request.CreateTemplateRequest;
import com.notificationservice.dto.request.CreateTenantRequest;
import com.notificationservice.dto.request.UpsertChannelConfigRequest;
import com.notificationservice.dto.request.UpsertRateLimitRequest;
import com.notificationservice.dto.response.TenantResponse;
import com.notificationservice.security.JwtUtil;
import com.notificationservice.security.SecurityContextHelper;
import com.notificationservice.service.ChannelConfigService;
import com.notificationservice.service.ReportService;
import com.notificationservice.service.TemplateService;
import com.notificationservice.service.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({TenantController.class, TemplateController.class, ChannelConfigController.class, ReportController.class, AuthController.class})
@AutoConfigureMockMvc(addFilters = false)
class OtherControllersTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private TenantService tenantService;
    @MockBean private TemplateService templateService;
    @MockBean private ChannelConfigService channelConfigService;
    @MockBean private ReportService reportService;
    @MockBean private com.notificationservice.service.AuthService authService;
    @MockBean private SecurityContextHelper securityHelper;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private com.notificationservice.repository.UserRepository userRepository;

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void testTenantController() throws Exception {
        CreateTenantRequest req = new CreateTenantRequest();
        req.setName("Test");
        req.setSlug("test");

        TenantResponse resp = TenantResponse.builder().id(UUID.randomUUID()).name("Test").build();
        when(tenantService.createTenant(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tenants"))
                .andExpect(status().isOk());
                
        UUID tenantId = UUID.randomUUID();
        when(tenantService.getRateLimitConfigs(tenantId)).thenReturn(List.of(new TenantRateLimitConfig()));
        
        mockMvc.perform(get("/api/v1/tenants/" + tenantId + "/rate-limits"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void testTemplateController() throws Exception {
        when(securityHelper.getCurrentTenantId()).thenReturn(UUID.randomUUID());

        CreateTemplateRequest req = new CreateTemplateRequest();
        req.setName("Welcome");
        req.setChannel(Channel.EMAIL);
        req.setSubject("Hello");
        req.setBodyTemplate("World");

        when(templateService.createTemplate(any(), any())).thenReturn(new NotificationTemplate());

        mockMvc.perform(post("/api/v1/templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/templates"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void testChannelConfigController() throws Exception {
        when(securityHelper.getCurrentTenantId()).thenReturn(UUID.randomUUID());

        UpsertChannelConfigRequest req = new UpsertChannelConfigRequest();
        req.setChannel(Channel.SMS);

        mockMvc.perform(put("/api/v1/channel-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/channel-configs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void testReportController() throws Exception {
        when(securityHelper.getCurrentTenantId()).thenReturn(UUID.randomUUID());
        when(reportService.getSummary(any(), any(), any())).thenReturn(Map.of("total", 100));

        mockMvc.perform(get("/api/v1/reports/summary?from=2024-01-01T00:00:00Z&to=2024-12-31T23:59:59Z"))
                .andExpect(status().isOk());
    }

    @Test
    void testAuthController() throws Exception {
        com.notificationservice.dto.request.LoginRequest req = new com.notificationservice.dto.request.LoginRequest();
        req.setEmail("test@test.com");
        req.setPassword("test");

        com.notificationservice.dto.response.AuthResponse resp = com.notificationservice.dto.response.AuthResponse.builder()
            .accessToken("token")
            .build();
        when(authService.login(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
