package com.yry.blog.myblogadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    private String username;
    private Long id;
    private String token_type = "Bearer";        // Token 类型，默认为 "Bearer"
    private String access_token;      // JWT Token
    private String refresh_token;
    private String expires_in;        // 过期时间
}
