package com.yry.blog.myblogarticle.vo;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data

public class CommentVO {
    private Long id;          // 评论ID，用于标识和可能的删除/编辑操作
    private String nickname;  // 用户昵称 (来自关联的 user 表)
    private String avatarUrl;    // 用户头像URL (来自关联的 user 表)
    private String content;   // 评论内容
    private LocalDateTime createdAt; // 创建时间
    private Long likeCount;   // 点赞数
    private Long parentId;    // 父评论ID，用于构建嵌套评论结构
    private List<CommentVO> children;
}
