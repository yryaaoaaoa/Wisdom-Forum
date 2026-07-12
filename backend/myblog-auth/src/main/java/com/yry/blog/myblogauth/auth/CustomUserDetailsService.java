package com.yry.blog.myblogauth.auth;

import com.yry.blog.myblogauth.service.PermissionService;
import com.yry.blog.myblogauth.service.UserService;
import com.yry.blog.myblogcommon.entity.user.User;
import com.yry.blog.myblogauth.mapper.UserAuthMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Objects; // 👈 导入 Objects
import java.util.stream.Collectors;

// ✅ 这个类是必须的！它是 Spring Security 的入口
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService userService;        // 你的业务 Service（查用户）
    @Autowired
    private PermissionService permissionService; // 查权限
    @Autowired
    private UserAuthMapper userMapper;
    @Override
    public CustomUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 调用业务 Service 查询用户
        User user = userService.findByUsername(username);
        if (user == null) throw new UsernameNotFoundException("用户不存在");
        // 2. 获取用户角色（通过你的业务逻辑）
        List<Long> roleId = userMapper.selectRoleIdsByUserId(user.getId()); // 获取用户角色ID列表
        // 3. 查询权限（通过你的业务逻辑）
        List<String> permissionCodes = permissionService.getPermissionCodesByUserId(user.getId());

        // 4. 构建权限列表 (✅ 添加过滤)
        List<GrantedAuthority> authorities = permissionCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 4. 构建 CustomUserDetails（包含业务字段）
        return CustomUserDetails.builder()
                .username(user.getUsername())
                .password(user.getPassword()) // 必须是加密后的
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .authorities(authorities) // 使用过滤后的 authorities
                .roleId(roleId)
                .build();
    }
    public CustomUserDetails buildUserDetails(User user) {
        List<Long> roleIds = userMapper.selectRoleIdsByUserId(user.getId());
        List<String> permissionCodes = permissionService.getPermissionCodesByUserId(user.getId());

        List<GrantedAuthority> authorities = permissionCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return CustomUserDetails.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .authorities(authorities)
                .roleId(roleIds)
                .build();
    }
}