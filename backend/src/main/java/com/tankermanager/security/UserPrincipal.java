package com.tankermanager.security;

import com.tankermanager.entity.UserAccount;
import com.tankermanager.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String phone;
    private final String passwordHash;
    private final Role role;
    private final Long operatorId;
    private final String fullName;
    private final boolean active;

    public UserPrincipal(UserAccount user) {
        this.id = user.getId();
        this.phone = user.getPhone();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.operatorId = user.getOperator() != null ? user.getOperator().getId() : null;
        this.fullName = user.getFullName();
        this.active = user.isActive();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
