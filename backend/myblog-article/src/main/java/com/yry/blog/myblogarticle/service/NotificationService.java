package com.yry.blog.myblogarticle.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yry.blog.myblogarticle.vo.NotificationVO;

public interface NotificationService {
    IPage<NotificationVO> getNotifications(Long userId, Integer page, Integer size);
    Integer getUnreadCount(Long userId);
    void markAsRead(Long id, Long userId);
    void markAllAsRead(Long userId);
    void sendLikeNotification(Long senderId, Long receiverId, Long articleId, String articleTitle);
}
