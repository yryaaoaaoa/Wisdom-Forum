package com.yry.blog.myblogarticle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yry.blog.myblogcommon.entity.article.Like;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LikeMapper extends BaseMapper<Like> {

    @Insert({
            "INSERT IGNORE INTO likes (user_id, target_id, like_type, create_time )",
            "VALUES (#{userId}, #{targetId}, #{likeType}, NOW())"
    })
    void insertIgnore(Like likesDO);

    @Delete({
            "DELETE FROM likes " +
            "WHERE user_id = #{userId} AND target_id = #{targetId} AND like_type = #{likeType}"
    })
    int deleteByUserAndTarget(
            @Param("userId") Long userId,
            @Param("targetId") Long targetId,
            @Param("likeType") Integer likeType
    );
}
