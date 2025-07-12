package com.example.soso.security.domain;

import com.example.soso.users.domain.entity.Users;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Users user;

    public CustomUserDetails(Users user) {
        this.user = user;
    }

    // 권한이 없는 경우 null 또는 빈 리스트
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    // 패스워드 인증 안 하므로 null
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return user.getId();
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
