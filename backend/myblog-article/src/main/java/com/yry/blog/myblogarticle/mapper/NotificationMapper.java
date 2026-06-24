package com.yry.blog.myblogarticle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yry.blog.myblogarticle.vo.NotificationVO;
import com.yry.blog.myblogcommon.entity.article.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Select("""
        SELECT 
            n.id,
            n.type,
            n.sender_id AS senderId,
            u.nickname AS senderName,
            u.avatar_url AS senderAvatar,
            n.target_id AS targetId,
            a.title AS targetTitle,
            n.content,
            n.is_read AS isRead,
            n.create_time AS createTime
        FROM notifications n
        LEFT JOIN users u ON n.sender_id = u.id
        LEFT JOIN articles a ON n.target_id = a.id
        WHERE n.user_id = #{userId}
        ORDER BY n.create_time DESC
        """)
    IPage<NotificationVO> selectNotificationPage(Page<NotificationVO> page, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM notifications WHERE user_id = #{userId} AND is_read = 0")
    Integer countUnread(@Param("userId") Long userId);

    @Update("UPDATE notifications SET is_read = 1 WHERE id = #{id} AND user_id = #{userId}")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE notifications SET is_read = 1 WHERE user_id = #{userId}")
    int markAllAsRead(@Param("userId") Long userId);
}
