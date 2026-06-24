package com.yry.blog.myblogarticle.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsVO {
    private Long articleCount;
    private Long categoryCount;
    private Long totalReadCount;
    private Long totalLikeCount;
    private Long userCount;
}
