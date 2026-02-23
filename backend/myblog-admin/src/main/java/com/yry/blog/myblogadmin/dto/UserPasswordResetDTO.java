package com.yry.blog.myblogadmin.dto;

import lombok.Data;

@Data
public class UserPasswordResetDTO {
    private String oldPassword;
    private String newPassword;
}
