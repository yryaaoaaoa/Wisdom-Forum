package com.yry.blog.myblogadmin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserAdminVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatarUrl;
    private Boolean enabled;
    private String roleName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
