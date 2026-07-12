package com.yry.blog.myblogauth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleVO {
    private Long id;
    private String roleName;
    private String roleDescription;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<String> permissions;
}
