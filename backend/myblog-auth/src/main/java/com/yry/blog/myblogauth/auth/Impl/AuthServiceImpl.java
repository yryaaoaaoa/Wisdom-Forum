package com.yry.blog.myblogauth.auth.Impl;

import com.yry.blog.myblogauth.auth.AuthService;
import com.yry.blog.myblogauth.dto.TokenRefreshResponseDTO;
import com.yry.blog.myblogauth.jwt.JwtUtils;
import com.yry.blog.myblogcommon.cache.manager.RedisCacheManager;
import com.yry.blog.myblogcommon.exception.BusinessException;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import static com.yry.blog.myblogcommon.enums.ResponseCodeEnums.INTERNAL_SERVER_ERROR;
import static com.yry.blog.myblogcommon.enums.ResponseCodeEnums.METHOD_NOT_ALLOWED;
import static com.yry.blog.myblogcommon.enums.ResponseCodeEnums.USELESS_TOKEN;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private JwtUtils jwtUtil;

    @Autowired
    private RedisCacheManager redisCacheManager;

    @Value("${spring.jwt.refreshExpireTime}") int refreshExpirationMs;


    @Override
    public TokenRefreshResponseDTO refreshToken(String refreshToken) {
        boolean isValid = jwtUtil.validateToken(refreshToken);
        if (!isValid) {
            throw new BusinessException(METHOD_NOT_ALLOWED);
        }

        Claims claims = jwtUtil.getClaims(refreshToken);
        String userName = claims.getSubject();
        Object userIdObj = claims.get("userId");
        Long userId;
        if (userIdObj instanceof Number) {
            userId = ((Number) userIdObj).longValue();
        } else {
            throw new BusinessException(METHOD_NOT_ALLOWED);
        }
        List<String> permissions = jwtUtil.extractPermissions(refreshToken);

        String redisKey = "refresh_token:" + refreshToken;
        String storedUsername = (String) redisCacheManager.get(redisKey);
        if (storedUsername == null || !storedUsername.equals(userName)) {
            throw new BusinessException(USELESS_TOKEN);
        }

        String newRefreshToken = jwtUtil.generateRefreshToken(userName, userId);
        String newAccessToken = jwtUtil.generateAccessToken(userName, userId, permissions);

        try {
            redisCacheManager.evict(redisKey);
            redisCacheManager.put("refresh_token:" + newRefreshToken, userName, refreshExpirationMs, TimeUnit.MILLISECONDS);
            String userRefreshTokensKey = "user:refresh_tokens:" + userName;
            redisCacheManager.put(userRefreshTokensKey, newRefreshToken, refreshExpirationMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Redis 存储 Token 失败，用户名: {}", userName, e);
            throw new BusinessException(INTERNAL_SERVER_ERROR, "Token存储失败，请稍后重试");
        }

        return new TokenRefreshResponseDTO(newAccessToken, newRefreshToken);
    }
}
