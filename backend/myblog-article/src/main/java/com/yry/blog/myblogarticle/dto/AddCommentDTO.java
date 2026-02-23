package com.yry.blog.myblogarticle.dto;

import lombok.Data;

@Data
public class AddCommentDTO {
    private String content;
    private Long parentId;
    private Long articleId;
}
