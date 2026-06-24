package com.yry.blog.myblogadmin.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * JWT工具类，负责JWT的生成、解析、验证等操作
 * 使用io.jsonwebtoken库实现，支持HMAC-SHA签名算法
 */
@Component
public class JwtUtils {
    private final byte[] secretKeyBytes;
    private final long expiration;
    private final long refreshexpiration;

    public JwtUtils(
            @Value("${spring.jwt.secret}") String secret,
            @Value("${spring.jwt.tokenExpireTime}") int accessExpireTime,
            @Value("${spring.jwt.refreshExpireTime}") int refreshExpireTime) {
        this.secretKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expiration = TimeUnit.MILLISECONDS.toMillis(accessExpireTime);
        this.refreshexpiration = TimeUnit.MILLISECONDS.toMillis(refreshExpireTime);
    }

    private String generateToken(String subject, long expirationMs, Map<String, Object> extraClaims) {
        return Jwts.builder()
                .setSubject(subject)
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .addClaims(extraClaims)
                .signWith(Keys.hmacShaKeyFor(secretKeyBytes))
                .compact();
    }

    public String generateAccessToken(String username, Long userId, List<String> permissionCodes) {
        Map<String, Object> claims = Map.of(
                "userId", userId,
                "permissions", permissionCodes
                // 可选: "roles", roleNames
        );
        return generateToken(username, expiration, claims);
    }

    public String generateRefreshToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return generateToken(username, refreshexpiration, claims);
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secretKeyBytes))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    public String extractUsername(String token) {
        return parseToken(token).getSubject();
    }

    public Date extractExpiration(String token) {
        return parseToken(token).getExpiration();
    }

    public List<String> extractPermissions(String token) {
        try {
            Claims claims = getClaims(token);
            Object permissions = claims.get("permissions");
            if (permissions instanceof List<?>) {
                return ((List<?>) permissions).stream()
                        .filter(item -> item instanceof String)
                        .map(item -> (String) item)
                        .toList();
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // 在 JwtUtils.java 中新增
    public Claims getClaims(String token) {
        try {
            // 正常解析（未过期）
            return parseToken(token);
        } catch (ExpiredJwtException e) {
            // 已过期，但我们可以从异常中获取 claims
            return e.getClaims();
        } catch (JwtException e) {
            return null;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        if (claims == null) {
            return null; // 无法解析 Claims
        }

        // 1. 提取基本信息
        String username = claims.getSubject(); // 从 sub claim 获取 username
        Object userIdObj = claims.get("userId");

        // 2. 验证并转换 userId
        Long userId;
        if (userIdObj instanceof Number) {
            userId = ((Number) userIdObj).longValue();
        } else {
            // Token 中缺少 userId 或格式不正确
            return null;
        }

        // 3. 提取权限
        List<String> permissions = extractPermissions(token);

        // 4. 转换为 GrantedAuthority 列表
        List<GrantedAuthority> authorities = permissions.stream()
                .filter(p -> p != null && !p.trim().isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 5. 构建包含详细信息的 principal Map
        Map<String, Object> principal = new HashMap<>();
        principal.put("username", username);
        principal.put("userId", userId);
        // 如果 Token 里还有其他信息，也可以添加
        // principal.put("nickname", claims.get("nickname"));

        // 6. 构建 Authentication 对象
        // principal: 我们构造的 Map 对象
        // credentials: JWT 无密码概念，设为 null
        // authorities: 从 Token 解析并转换的权限列表
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    /**
     * 从Token中提取用户ID（适配WebSocket握手拦截器使用）
     * @param token JWT Token（已去掉Bearer前缀）
     * @return 用户ID | null（Token无效/无userId时）
     */
    public Long getUserIdFromToken(String token) {
        try {
            // 1. 获取Claims（兼容过期Token，但WebSocket场景建议拒绝过期Token）
            Claims claims = getClaims(token);
            if (claims == null) {
                return null;
            }

            // 2. 校验Token是否过期（WebSocket场景强制要求未过期）
            Date expirationDate = claims.getExpiration();
            if (expirationDate.before(new Date())) {
                return null; // Token已过期，返回null
            }

            // 3. 提取并转换userId
            Object userIdObj = claims.get("userId");
            if (userIdObj instanceof Number) {
                return ((Number) userIdObj).longValue();
            } else {
                return null; // userId格式错误/不存在
            }
        } catch (Exception e) {
            // 所有异常都返回null，避免拦截器抛出异常
            return null;
        }
    }

    /**
     * 重载方法：从HttpServletRequest中提取Token并获取用户ID
     * （可选：用于非WebSocket场景，比如普通接口获取用户ID）
     * @param request HttpServletRequest
     * @return 用户ID | null
     */
    public Long getUserIdFromRequest(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return null;
        }
        return getUserIdFromToken(token);
    }
}