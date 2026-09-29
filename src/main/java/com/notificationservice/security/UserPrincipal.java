package com.notificationservice.security;

import com.notificationservice.domain.User;
import com.notificationservice.domain.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final UserRole role;
    private final UUID tenantId;
    private final boolean enabled;

    public UserPrincipal(User user) {
        this.id       = user.getId();
        this.email    = user.getEmail();
        this.password = user.getPasswordHash();
        this.role     = user.getRole();
        this.tenantId = user.getTenantId();
        this.enabled  = user.isEnabled();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getUsername()              { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return enabled; }

    public boolean isPlatformAdmin() { return role == UserRole.PLATFORM_ADMIN; }
    public boolean isTenantAdmin()   { return role == UserRole.TENANT_ADMIN; }
}
