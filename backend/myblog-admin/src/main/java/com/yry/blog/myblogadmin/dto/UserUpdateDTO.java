package com.yry.blog.myblogadmin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserUpdateDTO {
    private String nickname;
    private String email;
    private String avatar_url;
}
