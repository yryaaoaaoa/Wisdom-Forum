package com.yry.blog.myblogauth.dto;

import lombok.Data;

@Data
public class UserPasswordResetDTO {
    private String oldPassword;
    private String newPassword;
}
