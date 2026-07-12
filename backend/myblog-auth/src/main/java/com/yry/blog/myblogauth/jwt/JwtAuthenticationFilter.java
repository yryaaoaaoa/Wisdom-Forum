package com.yry.blog.myblogauth.jwt;
import java.io.IOException; // ✅ 正确的导入
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


/**
 * JWT 认证过滤器
 * 继承 OncePerRequestFilter 确保每次请求只执行一次过滤
 */
@Scope("prototype")
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 从请求中提取JWT Token
        String accessToken = jwtUtils.extractToken(request);
        // 2. 如果Token存在
        if (accessToken != null) {
            // 检查 Access Token 是否过期或者临期
            if (jwtUtils.validateToken(accessToken)) {
                // Token 有效，正常处理
                Authentication authentication = jwtUtils.getAuthentication(accessToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        // 3. 继续执行后续过滤器链
        filterChain.doFilter(request, response);
    }
}
