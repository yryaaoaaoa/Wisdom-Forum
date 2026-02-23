package com.yry.blog.myblogcommon.entity.article;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("likes")
public class Like implements Serializable {
    private Long id;
    private Long targetId;
    private Long userId;
    private LocalDateTime createTime;
    private Integer likeType;
}
