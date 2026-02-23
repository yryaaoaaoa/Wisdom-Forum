package com.yry.blog.myblogcommon.entity.RolePermission;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色权限关联实体类
 */
@Data
public class RolePermission {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 权限标识
     */
    private String permission;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
