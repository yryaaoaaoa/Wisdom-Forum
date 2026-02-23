package com.yry.blog.myblogarticle.mq;

import com.yry.blog.myblogarticle.mapper.ArticleMapper;
import com.yry.blog.myblogarticle.mapper.CommentMapper;
import com.yry.blog.myblogarticle.mapper.LikeMapper;
import com.yry.blog.myblogcommon.entity.article.Like;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        topic = "like-db-sync-topic",
        consumerGroup = "like-db-consumer-sync-group", // 改成和配置文件一致的名称
        consumeMode = ConsumeMode.CONCURRENTLY,
        maxReconsumeTimes = 3
)
@Slf4j
public class LikeDbSyncConsumer implements RocketMQListener<LikeSyncMessage> {
    @Autowired
    private LikeMapper likeMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private CommentMapper commentMapper;

    @Override
    public void onMessage(LikeSyncMessage message) {
        // 打印消费日志，便于排查问题
        log.info("开始消费点赞同步消息：userId={}, targetId={}, likeType={}, operation={}",
                message.getUserId(), message.getTargetId(), message.getLikeType(), message.getOperation());

        try {
            Long userId = message.getUserId();
            Long targetId = message.getTargetId();
            Integer likeType = message.getLikeType();
            Integer operation = message.getOperation();

            // 1. 校验必要参数，避免空指针
            if (userId == null || targetId == null || likeType == null || operation == null) {
                log.error("点赞同步消息参数为空：{}", message);
                return; // 无效消息，直接返回，不重试
            }

            // 2. 同步点赞表（幂等处理）
            if (operation == 1) {
                // 点赞：INSERT IGNORE 实现幂等，重复插入会忽略
                Like likesDO = new Like();
                likesDO.setUserId(userId);
                likesDO.setTargetId(targetId);
                likesDO.setLikeType(likeType);
                likeMapper.insertIgnore(likesDO);
                log.info("点赞记录插入成功：userId={}, targetId={}", userId, targetId);
            } else {
                // 取消点赞：删除记录，返回0是正常情况（重复消费），无需抛异常
                int deleteCount = likeMapper.deleteByUserAndTarget(userId, targetId, likeType);
                log.info("取消点赞记录：userId={}, targetId={}, 受影响行数={}", userId, targetId, deleteCount);
            }

            // 3. 同步文章/评论的点赞数（允许返回0，避免无效重试）
            if (likeType == 1) {
                int updateCount = articleMapper.updateLikeCount(targetId, operation);
                log.info("更新文章点赞数：targetId={}, operation={}, 受影响行数={}", targetId, operation, updateCount);
            } else if (likeType == 2) {
                int updateCount = commentMapper.updateLikeCount(targetId, operation);
                log.info("更新评论点赞数：targetId={}, operation={}, 受影响行数={}", targetId, operation, updateCount);
            }

        } catch (Exception e) {
            // 仅当出现数据库异常等严重错误时，抛出异常触发重试
            log.error("同步点赞数据到数据库失败，消息内容：{}", message, e);
            throw new RuntimeException("点赞数据同步失败，触发MQ重试", e);
        }
    }
}