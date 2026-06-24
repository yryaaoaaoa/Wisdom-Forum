package com.yry.blog.myblogadmin.auth;

import com.yry.blog.myblogadmin.dto.TokenRefreshResponseDTO;


public interface AuthService {
    TokenRefreshResponseDTO refreshToken(String refreshToken);
}
