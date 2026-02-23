package com.yry.blog.myblogwebsocket.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知消息实体
 */
@Data
public class NotificationMessage {
    // 消息类型：LIKE（点赞）、MENTION（@提醒）
    private String type;
    // 发送者ID/昵称
    private String senderName;
    // 接收者ID（@提醒时必填）
    private String receiverId;
    // 内容（如："xxx点赞了你的评论"、"xxx@了你在评论区的发言"）
    private String content;
    // 关联内容ID（如评论ID、文章ID，用于内容检索）
    private String contentId;
    // 消息时间
    private LocalDateTime createTime;
}