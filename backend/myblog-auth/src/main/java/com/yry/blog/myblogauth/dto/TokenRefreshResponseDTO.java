package com.yry.blog.myblogauth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshResponseDTO {
    private String access_token;
    private String refresh_token;
}
