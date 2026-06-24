package com.yry.blog.myblogadmin.aop;

import com.yry.blog.myblogcommon.annotation.RequiresPermission;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Aspect
@Component
public class PermissionAspect {

    // 定义切入点：拦截所有带有 @RequiresPermission 注解的方法
    @Pointcut("@annotation(requiresPermission)")
    public void permissionPointcut(RequiresPermission requiresPermission) {
        // 切入点方法体通常为空，它只是一个标识
    }

    // 定义前置通知，在目标方法执行前执行权限检查
    @Before("permissionPointcut(requiresPermission)")
    public void checkPermission(JoinPoint joinPoint, RequiresPermission requiresPermission) {
        // 1. 从 Spring Security 上下文获取当前已认证的用户信息
        Collection<? extends GrantedAuthority> authorities = getGrantedAuthorities();
        List<String> userPermissions = authorities.stream()
                .map(GrantedAuthority::getAuthority) // 获取权限字符串
                .toList();

        if (userPermissions.isEmpty()) {
            throw new AccessDeniedException("用户没有任何权限");
        }
        if (userPermissions.contains("system:admin")) {
            return; // 如果是超级管理员直接放行
        }

        // 3. 获取目标方法上 @RequiresPermission 注解所要求的权限
        String[] requiredPermissions = requiresPermission.value();

        // 4. 检查用户是否拥有所需的权限
        boolean hasPermission = Arrays.stream(requiredPermissions)
                .anyMatch(userPermissions::contains);

        if (!hasPermission) {
            // 如果没有权限，抛出 AccessDeniedException
            // Spring Security 的异常处理机制会捕获此异常，并返回标准的 403 Forbidden 响应
            throw new AccessDeniedException("权限不足，无法执行此操作");
        }

        // 5. 如果有权限，方法会继续正常执行
    }

    private static Collection<? extends GrantedAuthority> getGrantedAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 如果 authentication 为 null，说明用户未登录或 Token 无效
        if (authentication == null || !authentication.isAuthenticated()) {
            // 抛出 AccessDeniedException，Spring Security 会自动处理并返回 403
            throw new AccessDeniedException("用户未认证或 Token 已失效");
        }

        // 2. 获取用户的权限列表
        // authentication.getAuthorities() 返回的是一个 GrantedAuthority 集合
        // 我们需要将其转换为字符串列表，以便进行比较
        return authentication.getAuthorities();
    }
}