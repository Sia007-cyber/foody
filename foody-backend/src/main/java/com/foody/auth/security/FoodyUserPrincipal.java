package com.foody.auth.security;

import com.foody.users.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Bridges a Foody {@link User} to Spring Security's {@link UserDetails}.
 * The principal is carried in the security context after a valid JWT is verified.
 */
public class FoodyUserPrincipal implements UserDetails {

    private final User user;
    private final Long initiatingAdminId;
    private final String impersonationSessionId;

    public FoodyUserPrincipal(User user) {
        this(user, null, null);
    }

    public FoodyUserPrincipal(User user, Long initiatingAdminId, String impersonationSessionId) {
        this.user = user;
        this.initiatingAdminId = initiatingAdminId;
        this.impersonationSessionId = impersonationSessionId;
    }

    public User getUser() {
        return user;
    }

    public Long getUserId() {
        return user.getId();
    }

    public boolean isImpersonating() { return impersonationSessionId != null; }
    public Long getInitiatingAdminId() { return initiatingAdminId; }
    public String getImpersonationSessionId() { return impersonationSessionId; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != com.foody.users.entity.UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == com.foody.users.entity.UserStatus.ACTIVE;
    }
}
