package com.yry.blog.myblogadmin.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yry.blog.myblogadmin.service.EmailService;
import com.yry.blog.myblogadmin.service.PasswordResetService;
import com.yry.blog.myblogadmin.service.UserService;
import com.yry.blog.myblogcommon.cache.manager.RedisCacheManager;
import com.yry.blog.myblogcommon.entity.user.User;
import com.yry.blog.myblogcommon.enums.ResponseCodeEnums;
import com.yry.blog.myblogcommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String RESET_TOKEN_PREFIX = "password_reset:token:";
    private static final long TOKEN_EXPIRE_MINUTES = 30;
    
    private final RedisCacheManager redisCacheManager;
    private final EmailService emailService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${app.password-reset-url:http://localhost:8080/reset-password}")
    private String resetBaseUrl;

    public PasswordResetServiceImpl(RedisCacheManager redisCacheManager, 
                                     EmailService emailService, 
                                     UserService userService,
                                     PasswordEncoder passwordEncoder) {
        this.redisCacheManager = redisCacheManager;
        this.emailService = emailService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String createResetToken(String email) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String redisKey = RESET_TOKEN_PREFIX + token;
        
        redisCacheManager.put(redisKey, email, TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("创建密码重置Token，email: {}, token: {}", email, token);
        
        return token;
    }

    @Override
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        String redisKey = RESET_TOKEN_PREFIX + token;
        return redisCacheManager.hasKey(redisKey);
    }

    @Override
    public String getEmailByToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        String redisKey = RESET_TOKEN_PREFIX + token;
        Object email = redisCacheManager.get(redisKey);
        return email != null ? email.toString() : null;
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        String email = getEmailByToken(token);
        if (email == null) {
            throw new BusinessException(ResponseCodeEnums.BAD_REQUEST, "重置链接已过期或无效");
        }
        
        User user = userService.getOne(
            new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        );
        
        if (user == null) {
            throw new BusinessException(ResponseCodeEnums.USER_NOT_EXIST);
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateById(user);
        
        String redisKey = RESET_TOKEN_PREFIX + token;
        redisCacheManager.delete(redisKey);
        
        log.info("密码重置成功，userId: {}, email: {}", user.getId(), email);
    }

    @Override
    public void sendResetEmail(String username, String email) {
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new BusinessException(ResponseCodeEnums.USER_NOT_EXIST);
        }
        
        if (!email.equals(user.getEmail())) {
            throw new BusinessException(ResponseCodeEnums.EMAIL_ERROR, "邮箱与用户不匹配");
        }
        
        String token = createResetToken(email);
        emailService.sendPasswordResetEmail(email, token, resetBaseUrl);
        
        log.info("密码重置邮件已发送，username: {}, email: {}", username, email);
    }
}
