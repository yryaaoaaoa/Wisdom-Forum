package com.yry.blog.myblogcommon.entity.article;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("comments")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("article_id")
    private Long articleId;
    
    @TableField("content")
    private String content;
    
    @TableField("parent_id")
    private Long parentId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField("like_count")
    private Long likeCount;
    
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
