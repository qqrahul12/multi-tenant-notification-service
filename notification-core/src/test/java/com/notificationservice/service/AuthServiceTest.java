package com.notificationservice.service;

import com.notificationservice.domain.User;
import com.notificationservice.domain.UserRole;
import com.notificationservice.dto.request.LoginRequest;
import com.notificationservice.dto.response.AuthResponse;
import com.notificationservice.repository.UserRepository;
import com.notificationservice.security.JwtUtil;
import com.notificationservice.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @InjectMocks private AuthService authService;

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@test.com");
        req.setPassword("pass");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.PLATFORM_ADMIN);
        user.setEnabled(true);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken(any(UserPrincipal.class))).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(UserPrincipal.class))).thenReturn("refresh-token");

        AuthResponse resp = authService.login(req);

        assertEquals("access-token", resp.getAccessToken());
        assertEquals("refresh-token", resp.getRefreshToken());
        assertEquals("PLATFORM_ADMIN", resp.getRole());
    }

    @Test
    void shouldThrowWhenPasswordFails() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@test.com");
        req.setPassword("wrong");

        User user = new User();
        user.setPasswordHash("hashed");
        user.setEnabled(true);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }
}
