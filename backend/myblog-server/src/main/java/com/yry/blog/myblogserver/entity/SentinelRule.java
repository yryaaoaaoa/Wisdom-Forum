package com.yry.blog.myblogserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sentinel_rule")
public class SentinelRule {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String resource;

    private Integer grade;

    private Double count;

    @TableField("limit_app")
    private String limitApp;

    private Integer strategy;

    @TableField("control_behavior")
    private Integer controlBehavior;

    @TableField("warm_up_period_sec")
    private Integer warmUpPeriodSec;

    @TableField("max_queueing_time_ms")
    private Integer maxQueueingTimeMs;

    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
