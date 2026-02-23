package com.yry.blog.myblogadmin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserQueryDTO {
    private Integer current = 1;        // 当前页码
    private Integer size = 10;          // 每页大小
    private Boolean enabled;        // 是否启用
    private LocalDateTime startTime; // 创建时间范围开始
    private LocalDateTime endTime;   // 创建时间范围结束
    private String orderBy;  // 排序字段
    private String orderType;     // 排序方式
    private String keyword;
}

