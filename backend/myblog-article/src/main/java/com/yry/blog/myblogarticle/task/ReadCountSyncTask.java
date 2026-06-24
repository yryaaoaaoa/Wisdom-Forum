package com.yry.blog.myblogarticle.task;

import com.yry.blog.myblogarticle.mq.DefaultSendCallback;
import com.yry.blog.myblogarticle.mq.ReadCountSyncMessage;
import com.yry.blog.myblogcommon.stat.PostAccessCounter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class ReadCountSyncTask {

    private static final String READ_COUNT_SYNC_TOPIC = "read-count-sync-topic";
    private static final long SEND_TIMEOUT = 30000L;

    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private PostAccessCounter postAccessCounter;

    @Scheduled(fixedRate = 60000)
    public void syncReadCountToMySQL() {
        String keyPrefix = postAccessCounter.getReadCountKeyPrefix();
        Set<String> keys = redisTemplate.keys(keyPrefix + "*");

        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            try {
                String articleIdStr = key.replace(keyPrefix, "");
                Long articleId = Long.parseLong(articleIdStr);

                Integer readCount = redisTemplate.opsForValue().get(key);
                if (readCount == null || readCount <= 0) {
                    continue;
                }

                sendReadCountSyncMessage(articleId, readCount);

                redisTemplate.opsForValue().set(key, 0);

            } catch (Exception e) {
                log.error("同步阅读量失败，key: {}", key, e);
            }
        }
    }

    private void sendReadCountSyncMessage(Long articleId, Integer readCount) {
        ReadCountSyncMessage message = ReadCountSyncMessage.builder()
                .articleId(articleId)
                .readCount(readCount)
                .build();

        rocketMQTemplate.asyncSend(
                READ_COUNT_SYNC_TOPIC,
                message,
                new DefaultSendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("阅读量同步消息发送成功 | msgId:{} | articleId:{} | readCount:{}",
                                sendResult.getMsgId(), articleId, readCount);
                    }

                    @Override
                    public void onException(Throwable e) {
                        log.error("阅读量同步消息发送失败 | articleId:{} | readCount:{}",
                                articleId, readCount, e);
                    }
                },
                SEND_TIMEOUT
        );
    }
}
