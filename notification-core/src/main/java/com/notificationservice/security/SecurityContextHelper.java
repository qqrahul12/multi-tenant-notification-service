package com.notificationservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

/**
 * Helper to extract the current authenticated user from the SecurityContext.
 * Services use this instead of accepting tenantId as a parameter everywhere,
 * ensuring tenants can only access their own data (DIP — depends on abstraction).
 */
@Component
public class SecurityContextHelper {

    public UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new IllegalStateException("No authenticated user in SecurityContext");
    }

    public UUID getCurrentTenantId() {
        return getCurrentUser().getTenantId();
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public Optional<UUID> getCurrentTenantIdOptional() {
        try {
            return Optional.ofNullable(getCurrentTenantId());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public boolean isPlatformAdmin() {
        return getCurrentUser().isPlatformAdmin();
    }
}
