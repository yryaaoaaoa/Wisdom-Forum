package com.yry.blog.myblogarticle.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yry.blog.myblogarticle.mapper.NotificationMapper;
import com.yry.blog.myblogarticle.service.NotificationService;
import com.yry.blog.myblogarticle.vo.NotificationVO;
import com.yry.blog.myblogcommon.entity.article.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Override
    public IPage<NotificationVO> getNotifications(Long userId, Integer page, Integer size) {
        Page<NotificationVO> pageParam = new Page<>(page, size);
        return notificationMapper.selectNotificationPage(pageParam, userId);
    }

    @Override
    public Integer getUnreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    public void markAsRead(Long id, Long userId) {
        notificationMapper.markAsRead(id, userId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationMapper.markAllAsRead(userId);
    }

    @Override
    public void sendLikeNotification(Long senderId, Long receiverId, Long articleId, String articleTitle) {
        Notification notification = Notification.builder()
                .userId(receiverId)
                .type("LIKE")
                .senderId(senderId)
                .targetId(articleId)
                .content("点赞了你的文章《" + articleTitle + "》")
                .isRead(0)
                .build();
        
        notificationMapper.insert(notification);
        log.info("点赞通知已保存：senderId={}, receiverId={}, articleId={}", senderId, receiverId, articleId);
    }
}
