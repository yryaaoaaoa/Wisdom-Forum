package com.yry.blog.myblogarticle.vo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArticleVO {
    private Long id;
    private Long authId;
    private String authName;
    private String title;
    private String summary;
    private String coverUrl;
    private String content;
    private Integer status; // 或使用枚举类
    private Integer readCount;
    private Long likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
