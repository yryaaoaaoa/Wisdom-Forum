package com.yry.blog.myblogcommon.entity.Permission;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 权限实体类
 * 映射数据库 permission 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("permission")
public class Permission {
    /**
     * 权限ID
     * 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 权限标识（非空）
     * 如 user:view、user:create 等
     */
    @TableField("code")
    private String code;

    /**
     * 权限名称（非空）
     * 如 “查看用户”、“创建用户” 等
     */
    @TableField("name")
    private String name;

    /**
     * 创建时间
     * 插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 插入和更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

   }