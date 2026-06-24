package com.yry.blog.myblogarticle.service.Impl;

import com.yry.blog.myblogarticle.mapper.ArticleMapper;
import com.yry.blog.myblogarticle.mapper.LikeMapper;
import com.yry.blog.myblogarticle.mq.DefaultSendCallback;
import com.yry.blog.myblogarticle.service.LikeService;
import com.yry.blog.myblogarticle.service.NotificationService;
import com.yry.blog.myblogcommon.auth.PermissionChecker;
import com.yry.blog.myblogcommon.cache.manager.RedisCacheManager;
import com.yry.blog.myblogarticle.mq.LikeSyncMessage;
import com.yry.blog.myblogcommon.entity.article.Article;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class LikeServiceImpl implements LikeService {
    @Autowired
    private RedisCacheManager redisCacheManager;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private PermissionChecker permissionChecker;
    @Autowired
    private LikeMapper likeMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    @Qualifier("myblogTaskExecutor")
    private ThreadPoolExecutor myblogTaskExecutor;

    private static final String LIKE_SYNC_TOPIC = "like-db-sync-topic";
    public static final String LIKE_USERS_KEY = "like:users:%d:%d";
    public static final String LIKE_COUNT_KEY = "like:count:%d:%d";
    private static final long SEND_TIMEOUT = 30000L;

    @Resource(name = "likeRedisScript")
    private DefaultRedisScript<Long> likeRedisScript;

    @Override
    public boolean like(Long targetId, Integer likeType) {
        Long userId = permissionChecker.getCurrentUserId();
        if (userId == null) {
            log.error("点赞失败：用户未登录");
            return false;
        }

        String usersKey = String.format(LIKE_USERS_KEY, likeType, targetId);
        String countKey = String.format(LIKE_COUNT_KEY, likeType, targetId);

        try {
            List<String> keys = Arrays.asList(usersKey, countKey);
            Long result = redisCacheManager.getRedisTemplate().execute(
                    likeRedisScript,
                    keys,
                    userId.toString(),
                    "-1"
            );

            if (result == null) {
                log.error("点赞操作异常：脚本返回null，userId:{}, targetId:{}", userId, targetId);
                return false;
            }

            if (result == 1) {
                log.debug("用户{}点赞成功，targetId:{}", userId, targetId);
                sendLikeSyncMessage(userId, targetId, likeType, 1);
                sendLikeNotification(userId, targetId, likeType);
                return true;
            } else if (result == 2) {
                log.debug("用户{}取消点赞，targetId:{}", userId, targetId);
                sendLikeSyncMessage(userId, targetId, likeType, -1);
                return true;
            } else {
                log.debug("用户{}点赞操作失败，targetId:{}", userId, targetId);
                return false;
            }
        } catch (Exception e) {
            log.error("点赞操作异常，userId:{}, targetId:{}, likeType:{}", userId, targetId, likeType, e);
            return false;
        }
    }

    private void sendLikeNotification(Long senderId, Long targetId, Integer likeType) {
        if (likeType != 1) {
            return;
        }
        try {
            Article article = articleMapper.selectArticleInfoById(targetId);
            if (article != null && !article.getAuthId().equals(senderId)) {
                notificationService.sendLikeNotification(
                        senderId,
                        article.getAuthId(),
                        targetId,
                        article.getTitle()
                );
            }
        } catch (Exception e) {
            log.error("发送点赞通知失败，senderId:{}, targetId:{}", senderId, targetId, e);
        }
    }

    @Override
    public Long getLikeCount(Long targetId, Integer likeType) {
        String countKey = String.format(LIKE_COUNT_KEY, likeType, targetId);
        try {
            Object countObj = redisCacheManager.get(countKey);
            return countObj == null ? 0L : Long.parseLong(countObj.toString());
        } catch (Exception e) {
            log.warn("Redis异常，无法获取点赞数，targetId:{}", targetId);
            return 0L;
        }
    }

    @Override
    public boolean hasLiked(Long targetId, Integer likeType) {
        Long userId = permissionChecker.getCurrentUserId();
        if (userId == null) {
            return false;
        }
        String usersKey = String.format(LIKE_USERS_KEY, likeType, targetId);
        try {
            Boolean isMember = redisCacheManager.getRedisTemplate().opsForSet().isMember(usersKey, userId.toString());
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.warn("Redis异常，无法获取点赞状态，userId:{}, targetId:{}", userId, targetId);
            return false;
        }
    }

    @Override
    public Long getTotalLikeCount(Integer likeType) {
        try {
            return likeMapper.selectTotalLikeCountByType(likeType);
        } catch (Exception e) {
            log.warn("获取总点赞数失败，likeType:{}", likeType, e);
            return 0L;
        }
    }

    @Override
    public Map<Long, Long> batchGetLikeCount(List<Long> targetIds, Integer likeType) {
        Map<Long, Long> result = new HashMap<>();
        if (targetIds == null || targetIds.isEmpty()) {
            return result;
        }

        try {
            List<String> keys = targetIds.stream()
                    .map(id -> String.format(LIKE_COUNT_KEY, likeType, id))
                    .toList();

            List<Object> counts = redisCacheManager.getRedisTemplate().opsForValue().multiGet(keys);

            for (int i = 0; i < targetIds.size(); i++) {
                Long targetId = targetIds.get(i);
                Object countObj = counts != null && i < counts.size() ? counts.get(i) : null;
                result.put(targetId, countObj == null ? 0L : Long.parseLong(countObj.toString()));
            }
        } catch (Exception e) {
            log.warn("Redis批量获取点赞数失败，likeType:{}", likeType, e);
            for (Long targetId : targetIds) {
                result.put(targetId, 0L);
            }
        }

        return result;
    }

    public void sendLikeSyncMessage(Long userId, Long targetId, Integer likeType, Integer operation) {
        LikeSyncMessage message = new LikeSyncMessage();
        message.setUserId(userId);
        message.setTargetId(targetId);
        message.setLikeType(likeType);
        message.setOperation(operation);

        AtomicInteger retryCount = new AtomicInteger(3);
        doAsyncSend(message, retryCount);
    }

    private void doAsyncSend(LikeSyncMessage message, AtomicInteger retryCount) {
        rocketMQTemplate.asyncSend(
                LIKE_SYNC_TOPIC,
                message,
                new DefaultSendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("点赞消息发送成功 | msgId:{} | userId:{} | targetId:{}",
                                sendResult.getMsgId(), message.getUserId(), message.getTargetId());
                    }

                    @Override
                    public void onException(Throwable e) {
                        int remainingRetry = retryCount.decrementAndGet();
                        log.error("点赞消息发送失败，剩余重试次数：{}，userId:{}，targetId:{}",
                                remainingRetry, message.getUserId(), message.getTargetId(), e);

                        if (remainingRetry > 0 && isRetryableException(e)) {
                            myblogTaskExecutor.submit(() -> {
                                try {
                                    long sleepTime = (3 - remainingRetry) * 100L;
                                    Thread.sleep(sleepTime);
                                    doAsyncSend(message, retryCount);
                                } catch (InterruptedException ex) {
                                    Thread.currentThread().interrupt();
                                    log.error("点赞消息重试睡眠被中断 | userId:{}", message.getUserId(), ex);
                                }
                            });
                        } else {
                            log.error("点赞消息发送失败，重试耗尽 | userId:{} | targetId:{}",
                                    message.getUserId(), message.getTargetId(), e);
                        }
                    }
                },
                SEND_TIMEOUT
        );
    }

    private boolean isRetryableException(Throwable e) {
        String exceptionMsg = e.getMessage();
        return exceptionMsg != null && (
                exceptionMsg.contains("NetworkException") ||
                exceptionMsg.contains("BrokerNotAvailable") ||
                exceptionMsg.contains("Timeout") ||
                exceptionMsg.contains("RemotingException")
        );
    }
}
