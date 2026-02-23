package com.yry.blog.myblogarticle.dto;

import lombok.Data;

@Data
public class ArticleQueryDTO {
    private Integer sortOrder;
    private Long id;
    private String title;
    private String auth_id;
    private String status;
    private String authorName;
    private Long current;
    private Long size;
    private String tag;
}
