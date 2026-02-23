package com.yry.blog.myblogcommon.stat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 访问计数聚合定时任务
 */
@Slf4j
@Component
public class AccessCountScheduledTask {

    @Autowired
    private PostAccessCounter postAccessCounter;

    // 每分钟的第0秒执行（比如10:00:00、10:01:00）
    @Scheduled(cron = "0 * * * * ?")
    public void aggregateAccessCount() {
        try {
            postAccessCounter.aggregateToRedis();
        } catch (Exception e) {
            log.error("访问计数聚合任务执行失败", e);
        }
    }
}