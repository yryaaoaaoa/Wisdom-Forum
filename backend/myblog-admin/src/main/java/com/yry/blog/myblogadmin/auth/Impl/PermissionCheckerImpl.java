package com.yry.blog.myblogadmin.auth.Impl;

import com.yry.blog.myblogcommon.auth.PermissionChecker;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PermissionCheckerImpl implements PermissionChecker {

    @Override
    public boolean check(Long id) {
        Long currentUserId = getCurrentUserId();
        // Objects.equals 处理 null 情况
        return Objects.equals(currentUserId, id) || getPermissions(currentUserId).contains("system:admin");
    }

    @Override
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Map) {
            Object userIdObj = ((Map<?, ?>) auth.getPrincipal()).get("userId");
            if (userIdObj instanceof Number) {
                return ((Number) userIdObj).longValue();
            }
        }
        return null; // 或抛出异常，取决于你的需求
    }

    // 注意：此方法签名中的 Long userId 在基于JWT的权限检查中通常被忽略
    // 因为JWT只包含当前用户的权限。这里按你的接口定义保留。
    @Override
    public List<String> getPermissions(Long userId){
        return getCurrentUserPermissions(); // 忽略传入的 userId
    }

    /**
     * 获取当前认证用户的权限列表。
     * @return 权限列表，如果无法获取则返回空列表。
     */
    private List<String> getCurrentUserPermissions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Map) {
            Object permsObj = ((Map<?, ?>) auth.getPrincipal()).get("permissions");
            if (permsObj instanceof List<?>) {
                try {
                    @SuppressWarnings("unchecked")
                    List<String> permissions = (List<String>) permsObj;
                    return permissions;
                } catch (ClassCastException e) {
                    // 如果列表内元素不是String，返回空列表
                }
            }
        }
        return Collections.emptyList();
    }
}