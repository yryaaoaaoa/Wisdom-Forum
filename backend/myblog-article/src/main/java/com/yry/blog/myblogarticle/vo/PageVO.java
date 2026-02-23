package com.yry.blog.myblogarticle.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageVO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long authId;
    private String authName;
    private String title;
    private String summary;
    private String status; // 或使用枚举类
    private Integer readCount;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
