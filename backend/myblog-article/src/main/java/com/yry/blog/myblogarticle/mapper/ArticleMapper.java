package com.yry.blog.myblogarticle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yry.blog.myblogarticle.dto.ArticleQueryDTO;
import com.yry.blog.myblogarticle.dto.ArticleUpdateDTO;
import com.yry.blog.myblogarticle.vo.PageVO;
import com.yry.blog.myblogcommon.entity.article.Article; // 这是你的数据库实体
import com.yry.blog.myblogarticle.vo.ArticleVO; // 这是你的视图对象
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
    /**
     * 多表联合查询文章
     *
     * @param page      分页对象
     * @param queryDTO  封装了所有查询条件的 DTO 对象
     * @return IPage<ArticleVO>
     */

    IPage<ArticleVO> selectArticlePage(Page<PageVO> page, @Param("query") ArticleQueryDTO queryDTO);

    @Select("SELECT " +
            "a.id, " +
            "a.auth_id AS authId, " +
            "u.nickname AS authName, " + // 从users表取作者名称（根据实际字段调整，如username）
            "a.title, " +
            "a.summary, " +
            "a.content,"+
            "a.status, " +
            "a.read_count AS readCount, " +
            "a.like_count AS likeCount, " +
            "a.cover_url AS coverUrl, " +
            "a.created_at AS createdAt, " +
            "a.updated_at AS updatedAt " +
            "FROM articles a " +
            "LEFT JOIN users u ON a.auth_id = u.id " + // 左关联users表，避免作者信息缺失导致文章查不出来
            "WHERE a.auth_id = #{authId} AND a.status = 0")
    ArticleUpdateDTO  findDraftByAuthId(@Param("authId")Long authId);

    @Update("UPDATE articles " +
    "SET like_count = like_count + 1 "+
    "WHERE id = #{targetId} ")
    int updateLikeCount(Long targetId, Integer operation);
}