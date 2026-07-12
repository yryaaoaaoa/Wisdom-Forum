package com.yry.blog.myblogauth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResetPasswordDTO {
    
    @NotBlank(message = "Token不能为空")
    private String captcha;
    
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String newPassword;
    
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
