package com.yry.blog.myblogarticle.dto;

import lombok.Data;

@Data
public class ArticleQueryDTO {
    private Integer sortOrder;
    private Long id;
    private String title;
    private String auth_id;
    private Integer status;
    private String authorName;
    private Long current;
    private Long size;
    private String tag;
    private Long categoryId;
    private String keyword;
}
