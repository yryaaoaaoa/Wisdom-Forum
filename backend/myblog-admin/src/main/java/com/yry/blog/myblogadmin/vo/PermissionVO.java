package com.yry.blog.myblogadmin.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PermissionVO {
    private Long id;
    private String code;
    private String name;
    private LocalDateTime createTime;
}
