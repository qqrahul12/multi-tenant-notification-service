package com.notificationservice.service;

import com.notificationservice.domain.User;
import com.notificationservice.domain.UserRole;
import com.notificationservice.dto.request.CreateUserRequest;
import com.notificationservice.dto.request.UpdateDeviceTokenRequest;
import com.notificationservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(UUID tenantId, CreateUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with email already exists");
        }

        User user = new User();
        user.setTenantId(tenantId);
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDeviceToken(request.getDeviceToken());
        user.setRole(UserRole.USER);
        
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        } else {
            user.setPasswordHash(""); // dummy for now, since it might be required but this is just a device record
        }

        return userRepository.save(user);
    }

    @Transactional
    public void updateDeviceToken(UUID tenantId, UUID userId, UpdateDeviceTokenRequest request) {
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        user.setDeviceToken(request.getDeviceToken());
        userRepository.save(user);
    }
}
