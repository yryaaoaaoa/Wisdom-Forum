package com.yry.blog.myblogarticle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yry.blog.myblogcommon.entity.article.ArticleCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleCategoryMapper extends BaseMapper<ArticleCategory> {
    
    @Select("SELECT category_id FROM article_category WHERE article_id = #{articleId} LIMIT 1")
    Long selectCategoryIdByArticleId(@Param("articleId") Long articleId);
    
    @Select("SELECT article_id FROM article_category WHERE category_id = #{categoryId}")
    List<Long> selectArticleIdsByCategoryId(@Param("categoryId") Long categoryId);

    @Select("<script>" +
            "SELECT article_id, category_id FROM article_category " +
            "WHERE article_id IN " +
            "<foreach collection='articleIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Map<String, Object>> selectCategoryIdsByArticleIds(@Param("articleIds") List<Long> articleIds);

    @org.apache.ibatis.annotations.Delete("DELETE FROM article_category WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);
}
