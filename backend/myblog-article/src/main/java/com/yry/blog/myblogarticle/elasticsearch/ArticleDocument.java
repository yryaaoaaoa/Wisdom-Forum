package com.yry.blog.myblogarticle.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDocument {
    public static final String INDEX_NAME = "articles";

    private Long id;
    private String title;
    private String summary;
    private String content;
    private String authorName;
    private Long authId;
    private String coverUrl;
    private Integer status;
    private Integer readCount;
    private Long likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private Double hotScore;
    private Integer commentCount;
}
