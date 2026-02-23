package com.yry.blog.myblogadmin.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class UserAdminVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar_url;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
