package com.notificationservice.service;

import com.notificationservice.domain.User;
import com.notificationservice.dto.request.LoginRequest;
import com.notificationservice.dto.response.AuthResponse;
import com.notificationservice.repository.UserRepository;
import com.notificationservice.security.JwtUtil;
import com.notificationservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .filter(u -> u.isEnabled())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        UserPrincipal principal = new UserPrincipal(user);
        return AuthResponse.builder()
                .accessToken(jwtUtil.generateToken(principal))
                .refreshToken(jwtUtil.generateRefreshToken(principal))
                .tokenType("Bearer")
                .expiresIn(86400)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
