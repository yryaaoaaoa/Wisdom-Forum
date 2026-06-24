package com.yry.blog.myblogarticle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yry.blog.myblogarticle.dto.ArticleQueryDTO;
import com.yry.blog.myblogarticle.dto.ArticleUpdateDTO;
import com.yry.blog.myblogarticle.vo.PageVO;
import com.yry.blog.myblogcommon.entity.article.Article;
import com.yry.blog.myblogarticle.vo.ArticleVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    IPage<ArticleVO> selectArticlePage(Page<PageVO> page, @Param("query") ArticleQueryDTO queryDTO);

    @Select("SELECT " +
            "a.id, " +
            "a.auth_id AS authId, " +
            "u.nickname AS authName, " +
            "a.title, " +
            "a.summary, " +
            "a.content, " +
            "a.cover_url AS coverUrl, " +
            "a.status, " +
            "a.read_count AS readCount, " +
            "a.like_count AS likeCount, " +
            "a.created_at AS createdAt, " +
            "a.updated_at AS updatedAt, " +
            "ac.category_id AS categoryId " +
            "FROM articles a " +
            "LEFT JOIN users u ON a.auth_id = u.id " +
            "LEFT JOIN article_category ac ON a.id = ac.article_id " +
            "WHERE a.auth_id = #{authId} AND a.status = 0 AND a.is_deleted = 0")
    ArticleUpdateDTO findDraftByAuthId(@Param("authId")Long authId);

    @Update("UPDATE articles SET like_count = like_count + #{operation} WHERE id = #{targetId} AND is_deleted = 0")
    int updateLikeCount(@Param("targetId") Long targetId, @Param("operation") Integer operation);

    @Select("SELECT id, auth_id, title FROM articles WHERE id = #{id} AND is_deleted = 0")
    Article selectArticleInfoById(@Param("id") Long id);

    @Select("SELECT COALESCE(SUM(read_count), 0) FROM articles WHERE status = 1 AND is_deleted = 0")
    Long selectSumReadCount();

    @Select("SELECT COUNT(*) FROM users")
    Long selectUserCount();

    @Update("UPDATE articles SET read_count = read_count + #{count} WHERE id = #{articleId} AND is_deleted = 0")
    int updateReadCount(@Param("articleId") Long articleId, @Param("count") Integer count);

    @Select("SELECT " +
            "a.id, " +
            "a.auth_id AS authId, " +
            "u.nickname AS authName, " +
            "u.avatar_url AS authAvatar, " +
            "a.title, " +
            "a.summary, " +
            "a.content, " +
            "a.status, " +
            "a.read_count AS readCount, " +
            "a.like_count AS likeCount, " +
            "a.cover_url AS coverUrl, " +
            "a.created_at AS createdAt, " +
            "a.updated_at AS updatedAt " +
            "FROM articles a " +
            "LEFT JOIN users u ON a.auth_id = u.id " +
            "WHERE a.id = #{id} AND a.is_deleted = 0")
    Article selectArticleWithAuthorById(@Param("id") Long id);
}
