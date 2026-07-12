package com.yry.blog.myblogauth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserRegisterDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    @NotBlank(message = "昵称不能为空")
    private String nickname;
    @NotBlank(message = "邮箱不能为空")
    private String email;
    private String avatar_url;
    @NotBlank(message = "验证码不能为空")
    private String captcha;
}

