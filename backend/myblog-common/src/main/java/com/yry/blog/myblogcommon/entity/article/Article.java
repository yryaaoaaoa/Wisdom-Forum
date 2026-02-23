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
    @TableField("title")
    private String title;
    @TableField("summary")
    private String summary;
    @TableField("status")
    private Integer status; // 或使用枚举类
    @TableField("read_count")
    private Integer readCount;
    @TableField("like_count")
    private Integer likeCount;
    @TableField("cover_url")
    private String coverUrl;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField("content")
    private String content;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}