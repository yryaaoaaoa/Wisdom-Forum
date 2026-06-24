package com.yry.blog.myblogcommon.entity.article;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("articles")
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("auth_id")
    private Long authId;
    
    @TableField(exist = false)
    private String authName;
    
    @TableField(exist = false)
    private String authAvatar;
    
    @TableField("title")
    private String title;
    
    @TableField("summary")
    private String summary;
    
    @TableField("status")
    private Integer status;
    
    @TableField("read_count")
    private Integer readCount;
    
    @TableField("like_count")
    private Long likeCount;
    
    @TableField("cover_url")
    private String coverUrl;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField("content")
    private String content;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
