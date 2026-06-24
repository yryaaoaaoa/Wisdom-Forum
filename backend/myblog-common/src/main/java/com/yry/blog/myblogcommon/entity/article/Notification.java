package com.yry.blog.myblogcommon.entity.article;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("notifications")
public class Notification implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("type")
    private String type;
    
    @TableField("sender_id")
    private Long senderId;
    
    @TableField("target_id")
    private Long targetId;
    
    @TableField("content")
    private String content;
    
    @TableField("is_read")
    private Integer isRead;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
