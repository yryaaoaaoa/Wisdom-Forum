package com.yry.blog.myblogarticle.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticleUpdateDTO {
    private Long id;
    private Long categoryId;

    @Size(max = 100, message = "标题长度不能超过100字符")
    private String title;

    @Size(max = 100000, message = "内容长度不能超过100000字符")
    private String content;

    @Size(max = 200, message = "摘要长度不能超过200字符")
    private String summary;

    private String coverUrl;
    private Integer status;
}
