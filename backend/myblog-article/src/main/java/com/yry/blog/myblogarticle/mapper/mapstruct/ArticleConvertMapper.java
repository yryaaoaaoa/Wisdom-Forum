package com.yry.blog.myblogarticle.mapper.mapstruct;

import com.yry.blog.myblogarticle.dto.ArticleUpdateDTO;
import com.yry.blog.myblogcommon.entity.article.Article;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArticleConvertMapper {
    ArticleUpdateDTO convertToUpdateDTO(Article article);
}
