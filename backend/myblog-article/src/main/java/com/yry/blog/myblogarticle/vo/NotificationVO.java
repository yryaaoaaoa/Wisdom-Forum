package com.yry.blog.myblogarticle.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {
    private Long id;
    private String type;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private Long targetId;
    private String targetTitle;
    private String content;
    private Integer isRead;
    private LocalDateTime createTime;
}
