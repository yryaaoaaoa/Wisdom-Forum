package com.yry.blog.myblogarticle.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSearchDTO {
    private String keyword;
    private Long authId;
    @Builder.Default
    private Boolean sortByReadCount = false;
    @Builder.Default
    private Integer page = 1;
    @Builder.Default
    private Integer size = 10;
}
