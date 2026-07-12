package com.yry.blog.myblogauth.auth;

import com.yry.blog.myblogauth.dto.TokenRefreshResponseDTO;


public interface AuthService {
    TokenRefreshResponseDTO refreshToken(String refreshToken);
}
