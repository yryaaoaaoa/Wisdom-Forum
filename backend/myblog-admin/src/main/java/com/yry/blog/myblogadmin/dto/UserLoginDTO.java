package com.yry.blog.myblogadmin.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class UserLoginDTO {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank
    private String captcha;
    private String captchaKey;
}
