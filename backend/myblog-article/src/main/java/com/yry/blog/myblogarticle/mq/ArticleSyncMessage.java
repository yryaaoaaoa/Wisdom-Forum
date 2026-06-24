package com.yry.blog.myblogarticle.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSyncMessage implements Serializable {
    private Long articleId;
    private String operation;
}
