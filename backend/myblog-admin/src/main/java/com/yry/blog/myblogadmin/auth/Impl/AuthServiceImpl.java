package com.yry.blog.myblogadmin.auth.Impl;

import com.yry.blog.myblogadmin.auth.AuthService;
import com.yry.blog.myblogadmin.jwt.JwtUtils;
import com.yry.blog.myblogcommon.cache.manager.RedisCacheManager;
import com.yry.blog.myblogcommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import static com.yry.blog.myblogcommon.enums.ResponseCodeEnums.METHOD_NOT_ALLOWED;
import static com.yry.blog.myblogcommon.enums.ResponseCodeEnums.USELESS_TOKEN;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private JwtUtils jwtUtil; // 自定义 JWT 工具类（生成/解析 Token）

    @Autowired
    private RedisCacheManager redisCacheManager;

    @Value("${spring.jwt.refreshExpireTime}") int refreshExpirationMs;


    @Override
    public String refreshToken(String refreshToken) {
        // 1. 校验 refreshToken 合法性（解析 JWT、判断是否篡改）
        boolean isValid = jwtUtil.validateToken(refreshToken);
        if (!isValid) {
            throw new BusinessException(METHOD_NOT_ALLOWED);
        }

        // 2. 从 Token 中解析用户信息
        Long userId = Long.valueOf(jwtUtil.getClaims(refreshToken).getId());
        String userName = jwtUtil.getClaims(refreshToken).getSubject();
        List<String> permissions = jwtUtil.extractPermissions(refreshToken);

        // 3. 校验 refreshToken 是否在存储中存在（防止已注销的 Token 被复用）
        String redisKey = "refresh_token:" + userId;
        String storedRefreshToken = (String) redisCacheManager.get(redisKey);
        if (!refreshToken.equals(storedRefreshToken)) {
            throw new BusinessException(USELESS_TOKEN);
        }
        // 4. 删除redis中原先的refresh_token，生成并存入新的refresh_token
        String newRefreshToken = jwtUtil.generateRefreshToken(userName);
        try {
            // 删除原先的refresh_token
            redisCacheManager.evict(refreshToken);
            // 维护新Refresh Token 和用户的映射
            redisCacheManager.put("refresh_token:" + newRefreshToken, userName,refreshExpirationMs, TimeUnit.MILLISECONDS);
            // 维护用户与新Refresh Token 的映射
            String userRefreshTokensKey = "user:refresh_tokens:" + userName;
            redisCacheManager.put(userRefreshTokensKey, newRefreshToken,refreshExpirationMs, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            log.error("Redis 存储 Token 失败，用户名: {}", userName, e);
            return null;
        }
        // 5. 返回新 Token 封装
        return jwtUtil.generateAccessToken(userName,userId,permissions);
    }
}
