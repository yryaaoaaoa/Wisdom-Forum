package com.yry.blog.myblogadmin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class UserUpdateDTO {
    private String nickname;
    private String email;
    private String avatarUrl;
    private Boolean enabled;
    private List<String> roles;
}
