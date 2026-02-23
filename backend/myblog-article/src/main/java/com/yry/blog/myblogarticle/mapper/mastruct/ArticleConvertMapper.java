package com.yry.blog.myblogarticle.mapper.mastruct;

import com.yry.blog.myblogarticle.dto.ArticleUpdateDTO;
import com.yry.blog.myblogcommon.entity.article.Article;
import org.mapstruct.Mapper;

// 注意，org.mapstruct.Mapper 注解实例创建的是一个普通的 Java 对象，没有被 Spring 容器管理，所以这里要加上componentModel = "spring"
@Mapper(componentModel = "spring")
public interface ArticleConvertMapper {
    ArticleUpdateDTO convertToUpdateDTO(Article article);
}
