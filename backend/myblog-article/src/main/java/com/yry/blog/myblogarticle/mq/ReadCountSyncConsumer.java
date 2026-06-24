package com.yry.blog.myblogarticle.mq;

import com.yry.blog.myblogarticle.mapper.ArticleMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        topic = "read-count-sync-topic",
        consumerGroup = "read-count-db-sync-group",
        consumeMode = ConsumeMode.CONCURRENTLY,
        maxReconsumeTimes = 3
)
@Slf4j
public class ReadCountSyncConsumer implements RocketMQListener<ReadCountSyncMessage> {

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public void onMessage(ReadCountSyncMessage message) {
        log.info("开始消费阅读量同步消息：articleId={}, readCount={}",
                message.getArticleId(), message.getReadCount());

        try {
            Long articleId = message.getArticleId();
            Integer readCount = message.getReadCount();

            if (articleId == null || readCount == null || readCount <= 0) {
                log.error("阅读量同步消息参数无效：{}", message);
                return;
            }

            int updateCount = articleMapper.updateReadCount(articleId, readCount);
            log.info("更新文章阅读量：articleId={}, readCount={}, 受影响行数={}", 
                    articleId, readCount, updateCount);

        } catch (Exception e) {
            log.error("同步阅读量到数据库失败，消息内容：{}", message, e);
            throw new RuntimeException("阅读量同步失败，触发MQ重试", e);
        }
    }
}
