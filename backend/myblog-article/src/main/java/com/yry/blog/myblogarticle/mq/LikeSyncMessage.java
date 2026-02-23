package com.yry.blog.myblogarticle.mq;

import lombok.Data;

import java.io.Serializable;

@Data
public class LikeSyncMessage implements Serializable {
    private Long userId; // 点赞用户ID
    private Long targetId; // 文章/评论ID
    private Integer likeType; // 1=文章 2=评论
    private Integer operation; // 1=点赞 -1=取消点赞
}
