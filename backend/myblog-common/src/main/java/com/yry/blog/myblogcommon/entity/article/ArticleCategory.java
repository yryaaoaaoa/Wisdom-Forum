package com.yry.blog.myblogcommon.entity.article;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("article_category")
public class ArticleCategory implements Serializable {
    
    @TableField("article_id")
    private Long articleId;
    
    @TableField("category_id")
    private Long categoryId;
}
