package com.yry.blog.myblogadmin.controller;

import com.yry.blog.myblogadmin.auth.AuthService;
import com.yry.blog.myblogadmin.dto.LoginResponseDTO;
import com.yry.blog.myblogadmin.dto.UserLoginDTO;
import com.yry.blog.myblogadmin.service.UserLoginService;
import com.yry.blog.myblogcommon.captcha.CaptchaService;
import com.yry.blog.myblogcommon.captcha.dto.CaptchaResponse;
import com.yry.blog.myblogcommon.result.Response;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserLoginService userLoginService;
    private final CaptchaService captchaService;

    public AuthController(UserLoginService userLoginService, CaptchaService captchaService, AuthService authService) {
        this.userLoginService = userLoginService;
        this.captchaService = captchaService;
        this.authService = authService;
    }
    @PostMapping("/login")
    public Response<LoginResponseDTO> login(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        return userLoginService.login(userLoginDTO);
    }
    @PostMapping("/logout")
    public Response<Object> logout(@RequestHeader("Authorization") String refreshToken) {
        // 如果格式是 "Bearer <token>"，需要解析
        if (refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }
        return userLoginService.logout(refreshToken);
    }
    @GetMapping("/captcha")
    public Response<CaptchaResponse> getCaptcha() {
        return Response.success(captchaService.generateCaptcha());
    }

    @GetMapping("/refresh")
    public String getAccessToken(@RequestParam String refreshToken){
        return authService.refreshToken(refreshToken);
    }
}
