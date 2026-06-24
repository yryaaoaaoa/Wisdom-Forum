package com.yry.blog.myblogarticle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yry.blog.myblogcommon.entity.article.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
    
    @Select("SELECT c.* FROM categories c " +
            "INNER JOIN article_category ac ON c.id = ac.category_id " +
            "WHERE ac.article_id = #{articleId}")
    List<Category> selectByArticleId(Long articleId);
}
