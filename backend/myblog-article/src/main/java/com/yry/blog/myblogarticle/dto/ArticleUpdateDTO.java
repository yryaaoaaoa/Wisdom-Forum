package com.yry.blog.myblogarticle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticleUpdateDTO {
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 10000, message = "内容长度不能超过10000字符")
    private String content;

    @Size(max = 200, message = "摘要长度不能超过200字符")
    private String summary;
}
