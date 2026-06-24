package com.yry.blog.myblogadmin.controller;

import com.yry.blog.myblogadmin.auth.AuthService;
import com.yry.blog.myblogadmin.dto.*;
import com.yry.blog.myblogadmin.service.PasswordResetService;
import com.yry.blog.myblogadmin.service.UserLoginService;
import com.yry.blog.myblogadmin.service.UserService;
import com.yry.blog.myblogcommon.captcha.CaptchaService;
import com.yry.blog.myblogcommon.captcha.dto.CaptchaResponse;
import com.yry.blog.myblogcommon.enums.ResponseCodeEnums;
import com.yry.blog.myblogcommon.exception.BusinessException;
import com.yry.blog.myblogcommon.result.Response;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserLoginService userLoginService;
    private final CaptchaService captchaService;
    private final UserService userService;
    private final PasswordResetService passwordResetService;

    public AuthController(UserLoginService userLoginService, CaptchaService captchaService, 
                          AuthService authService, UserService userService,
                          PasswordResetService passwordResetService) {
        this.userLoginService = userLoginService;
        this.captchaService = captchaService;
        this.authService = authService;
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }
    
    @PostMapping("/login")
    public Response<LoginResponseDTO> login(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        return userLoginService.login(userLoginDTO);
    }
    
    @PostMapping("/logout")
    public Response<Object> logout(@RequestHeader("Authorization") String refreshToken) {
        if (refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }
        return userLoginService.logout(refreshToken);
    }
    
    @GetMapping("/captcha")
    public Response<CaptchaResponse> getCaptcha() {
        return Response.success(captchaService.generateCaptcha());
    }

    @PostMapping("/refresh")
    public Response<TokenRefreshResponseDTO> getAccessToken(@RequestBody Map<String, String> request){
        String refreshToken = request.get("refresh_token");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Response.error(com.yry.blog.myblogcommon.enums.ResponseCodeEnums.BAD_REQUEST);
        }
        return Response.success(authService.refreshToken(refreshToken));
    }
    
    @PostMapping("/register")
    public Response<Object> register(@Valid @RequestBody RegisterDTO registerDTO) {
        if (!captchaService.validateCaptcha(registerDTO.getCaptchaKey(), registerDTO.getCaptcha())) {
            throw new BusinessException(ResponseCodeEnums.CAPTCHA_ERROR);
        }
        
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException(ResponseCodeEnums.BAD_REQUEST, "两次密码输入不一致");
        }
        
        UserRegisterDTO userRegisterDTO = new UserRegisterDTO();
        userRegisterDTO.setUsername(registerDTO.getUsername());
        userRegisterDTO.setPassword(registerDTO.getPassword());
        userRegisterDTO.setNickname(registerDTO.getNickname());
        userRegisterDTO.setEmail(registerDTO.getEmail());
        
        userService.addUser(userRegisterDTO);
        return Response.success("注册成功");
    }
    
    @PostMapping("/forget-password")
    public Response<String> forgetPassword(@Valid @RequestBody ForgetPasswordDTO dto) {
        if (!captchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptcha())) {
            throw new BusinessException(ResponseCodeEnums.CAPTCHA_ERROR);
        }
        
        passwordResetService.sendResetEmail(dto.getUsername(), dto.getEmail());
        return Response.success("密码重置邮件已发送，请查收邮件");
    }
    
    @GetMapping("/validate-reset-token")
    public Response<Object> validateResetToken(@RequestParam String token) {
        boolean valid = passwordResetService.validateToken(token);
        if (valid) {
            return Response.success("Token有效");
        } else {
            return Response.error(ResponseCodeEnums.BAD_REQUEST, "重置链接已过期或无效");
        }
    }
    
    @PostMapping("/reset-password")
    public Response<String> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ResponseCodeEnums.BAD_REQUEST, "两次密码输入不一致");
        }
        
        passwordResetService.resetPassword(dto.getCaptcha(), dto.getNewPassword());
        return Response.success("密码重置成功，请使用新密码登录");
    }
}
