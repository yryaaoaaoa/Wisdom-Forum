package com.yry.blog.myblogauth.auth.Impl;

import com.yry.blog.myblogcommon.auth.PermissionChecker;
import com.yry.blog.myblogcommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.yry.blog.myblogcommon.enums.ResponseCodeEnums.UNAUTHORIZED;

@Slf4j
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
        log.warn("无法获取当前用户ID：用户未认证或Token无效");
        throw new BusinessException(UNAUTHORIZED, "用户未认证或Token无效");
    }

    /**
     * 获取用户权限列表。
     * 注意：在基于JWT的实现中，userId参数被忽略，因为JWT中只包含当前用户的权限。
     * 这是设计决策，因为JWT是无状态的，无法根据userId查询其他用户的权限。
     * 
     * @param userId 用户ID（在JWT实现中被忽略）
     * @return 当前认证用户的权限列表
     */
    @Override
    public List<String> getPermissions(Long userId){
        return getCurrentUserPermissions(); // 基于JWT实现，忽略传入的 userId
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
                    log.warn("权限列表类型转换失败", e);
                }
            }
        }
        return Collections.emptyList();
    }
}