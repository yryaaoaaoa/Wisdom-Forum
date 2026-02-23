package com.yry.blog.myblogcommon.entity.Role;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 角色实体类
 *
 * @TableName 注解指定了数据库中的表名，假设你的表名叫 'role'
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("role")
public class Role {
    /**
     * 角色ID
     *
     * @TableId 注解标识该字段为主键
     * type = IdType.AUTO 指定主键为自增类型
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色名称 (例如: ROLE_ADMIN, ROLE_USER)
     *
     * @TableField 注解指定该字段对应数据库表中的列名
     * 如果实体类字段名和数据库列名一致，可以省略，但显式写出更佳
     */
    @TableField("role_name")
    private String roleName;

    /**
     * 角色描述
     */
    @TableField("role_description")
    private String roleDescription;

    /**
     * 创建时间
     *
     * @TableField(fill = FieldFill.INSERT) 表示在插入数据时，由MyBatis-Plus自动填充
     * 需要配合 MetaObjectHandler 接口的实现类来使用
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     *
     * @TableField(fill = FieldFill.INSERT_UPDATE) 表示在插入和更新数据时，由MyBatis-Plus自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记 (0-未删除, 1-已删除)
     *
     * @TableLogic 注解用于标识逻辑删除字段
     * 这是一个非常实用的功能，可以在不真正删除数据的情况下实现删除效果
     */
}