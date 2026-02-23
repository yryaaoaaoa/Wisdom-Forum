package com.yry.blog.myblogadmin.auth;

import org.springframework.stereotype.Service;


public interface AuthService {
    public String refreshToken(String refreshToken);
}
