package com.yry.blog.myblogarticle.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleContentVO {

    private Long id;
    private Long authId;
    private String authorNickname;
    private String authorAvatarUrl;
    private String title;
    private String contentUrl;
    private String summary;
    private Integer readCount;
    private Integer likeCount;
    private String coverUrl;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}