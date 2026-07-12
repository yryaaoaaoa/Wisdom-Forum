package com.yry.blog.myblogauth.service.Impl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yry.blog.myblogauth.auth.CustomUserDetails;
import com.yry.blog.myblogauth.auth.CustomUserDetailsService;
import com.yry.blog.myblogauth.dto.LoginResponseDTO;
import com.yry.blog.myblogauth.dto.UserLoginDTO;
import com.yry.blog.myblogcommon.cache.manager.RedisCacheManager;
import com.yry.blog.myblogcommon.cache.util.CacheUtil;
import com.yry.blog.myblogcommon.captcha.CaptchaService;
import com.yry.blog.myblogcommon.entity.user.User;
import com.yry.blog.myblogauth.service.UserLoginService;
import com.yry.blog.myblogauth.mapper.UserAuthMapper;
import com.yry.blog.myblogcommon.result.Response;
import com.yry.blog.myblogauth.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import static com.yry.blog.myblogcommon.enums.ResponseCodeEnums.*;

@Slf4j
@Service
@RequiredArgsConstructor // Lombok自动生成构造函数
public class UserLoginServiceImpl implements UserLoginService {
    private final RedisTemplate<Object, Object> redisTemplate;
    @Value("${spring.jwt.refreshExpireTime}") long refreshExpirationMs;
    private final UserAuthMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final CacheUtil cacheUtil;
    private final CaptchaService captchaService;
    private final CustomUserDetailsService customUserDetailsService;
    private final RedisCacheManager redisCacheManager;


    @Override
    public Response<LoginResponseDTO> login(UserLoginDTO userLoginDTO) {
        // 1. 查询用户
        User user = userMapper.selectOne(new QueryWrapper<User>()
                .eq("username", userLoginDTO.getUsername()));
        if (user == null) {
            return Response.error(USER_NOT_EXIST);
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(userLoginDTO.getPassword(), user.getPassword())) {
            return Response.error(USERNAME_OR_PASSWORD_ERROR);
        }
        // 3. 验证验证码
        String captchaKey = userLoginDTO.getCaptchaKey();  // 验证码唯一标识
        String userInput = userLoginDTO.getCaptcha();      // 用户输入的验证码
        if (!captchaService.validateCaptcha(captchaKey, userInput)) {
            return Response.error(CAPTCHA_ERROR);
        }
        // 4. 验证用户状态
        if (!user.isEnabled()) {
            return Response.error(USER_DISABLED);
        }
        // 5. 构建用户详情

        //  获取用户所有角色
        CustomUserDetails userDetails = customUserDetailsService.buildUserDetails(user);
        List<String> permissionCodes = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        // 6. 生成 AccessToken和RefreshToken
        String username = userDetails.getUsername();
        Long userId = userDetails.getUserId();
        String accessToken = jwtUtils.generateAccessToken(username,userId,permissionCodes);
        String refreshToken = jwtUtils.generateRefreshToken(username, userId);
        String tokenType = "Bearer";
        String expiresIn = jwtUtils.extractExpiration(accessToken).toString();


        // 7. 存储到 Redis（关键操作，失败应终止登录）
        try {
            // 维护 Refresh Token 和用户的映射
            redisCacheManager.put("refresh_token:" + refreshToken, username,refreshExpirationMs,TimeUnit.MILLISECONDS);
            // 维护用户与 Refresh Token 的映射
            String userRefreshTokensKey = "user:refresh_tokens:" + username;
            redisCacheManager.put(userRefreshTokensKey, refreshToken,refreshExpirationMs, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            log.error("Redis 存储 Token 失败，用户名: {}", username, e);
            return Response.error(INTERNAL_SERVER_ERROR);
        }

        // 设置安全上下文
        return Response.success(new LoginResponseDTO(
                username,
                user.getId(),
                tokenType,
                accessToken,
                refreshToken,
                expiresIn
        ));
    }

    @Override
    public Response<Object> logout(String refreshToken) {
        try {
            // 1. 从 Redis 中删除 Refresh Token
            String refreshRedisKey = "refresh_token:" + refreshToken;
            String username = (String) redisCacheManager.get(refreshRedisKey); // refresh_token 是安全凭证，要求强一致性

            if (username != null) {
                // 删除 Refresh Token
                redisTemplate.delete(refreshRedisKey);

                // 删除用户关联的 Refresh Token 记录
                String userRefreshTokensKey = "user:refresh_tokens:" + username;
                cacheUtil.removeFromSet(userRefreshTokensKey, refreshToken);
            }

            return Response.success(null);
        } catch (Exception e) {
            log.error("登出失败，RefreshToken: {}", refreshToken, e);
            return Response.error(INTERNAL_SERVER_ERROR);
        }
    }

}