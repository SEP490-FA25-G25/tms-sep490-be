package org.fyp.tmssep490be.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.UserRole;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private Long id;
    private String email;
    private String passwordHash;
    private String fullName;
    private UserStatus status;
    private Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(UserAccount user) {
        Collection<GrantedAuthority> authorities = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .collect(Collectors.toList());

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getStatus(),
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
