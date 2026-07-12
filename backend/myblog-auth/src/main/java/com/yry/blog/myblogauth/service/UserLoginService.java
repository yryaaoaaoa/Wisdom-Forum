package com.yry.blog.myblogauth.service;

import com.yry.blog.myblogauth.dto.LoginResponseDTO;
import com.yry.blog.myblogauth.dto.UserLoginDTO;
import com.yry.blog.myblogcommon.result.Response;

import java.util.Map;

public interface UserLoginService {
    Response<LoginResponseDTO> login(UserLoginDTO userLoginDTO);

    /**
     * 用户登出
     * @param refreshToken 当前用户的 refreshToken
     */
    Response<Object> logout(String refreshToken);

    /**
     * 刷新 token
     * @param refreshToken 刷新 token
     * @return 新的 token 信息
     */
}
