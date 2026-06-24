package com.yry.blog.myblogcommon.entity.RolePermission;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("role_permission")
public class RolePermission {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("role_id")
    private Long roleId;
    
    @TableField("permission")
    private String permission;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
