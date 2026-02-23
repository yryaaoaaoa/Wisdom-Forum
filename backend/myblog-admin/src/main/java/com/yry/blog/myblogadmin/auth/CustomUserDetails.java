package com.yry.blog.myblogadmin.auth;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Builder
@Data
public class CustomUserDetails implements UserDetails {

    // === Spring Security 核心字段 ===
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    // 账户状态字段 - 设置默认值
    @Builder.Default
    private final boolean accountNonExpired = true;

    @Builder.Default
    private final boolean accountNonLocked = true;

    @Builder.Default
    private final boolean credentialsNonExpired = true;

    @Builder.Default
    private final boolean enabled = true;

    // === 业务扩展字段 ===
    private final Long userId;
    private final String nickname;
    private final String email;
    private final List<Long> roleId;

    // 注意：不需要显式编写构造方法，Lombok 会处理

    // UserDetails 接口方法实现
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
    // 其他 getter 方法保持不变...
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

}