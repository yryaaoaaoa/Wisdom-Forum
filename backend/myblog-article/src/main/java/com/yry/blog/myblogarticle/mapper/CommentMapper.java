package com.yry.blog.myblogarticle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yry.blog.myblogarticle.vo.CommentVO;
import com.yry.blog.myblogcommon.entity.article.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    @Select("SELECT \n" +
            "    c.id,\n" +
            "    c.content,\n" +
            "    c.parent_id,\n" +
            "    c.created_at,\n" +
            "    c.like_count,\n" +
            "    u.nickname,\n" +
            "    u.avatar_url AS avatarUrl\n" +
            "FROM comments c\n" +
            "JOIN users u ON c.user_id = u.id\n" +
            "WHERE c.article_id = #{articleId}\n"+
            "AND c.is_deleted = 0\n"+
            "ORDER BY created_at ASC")
    List<CommentVO> getComment(@Param("articleId") Long articleId);

    @Update("UPDATE comments SET like_count = like_count + #{operation} WHERE id = #{targetId} AND is_deleted = 0")
    int updateLikeCount(@Param("targetId") Long targetId, @Param("operation") Integer operation);

    @Select("SELECT COUNT(*) FROM comments WHERE article_id = #{articleId} AND is_deleted = 0")
    int countByArticleId(@Param("articleId") Long articleId);
}
